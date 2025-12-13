package cat.dog.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChromaExtractedFaceImporter {

    private static final String VENV_DIR_NAME = ".venv";
    private static final String PICKLE_FILE = "./../../DATA/meme_face_embeddings_mobileclip.pkl"; 
    private static final String TARGET_COLLECTION_NAME = "face_vectors";
    
    // Port 8001 (Docker)
    private static final String CHROMA_BASE_URL = "http://localhost:8001/api/v1";

    private static final String PYTHON_EXEC;
    private static final String PIP_EXEC;
    private static final String SYS_PYTHON;

    static {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "Scripts" + File.separator + "python.exe";
            PIP_EXEC = VENV_DIR_NAME + File.separator + "Scripts" + File.separator + "pip.exe";
            SYS_PYTHON = "python";
        } else {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "bin" + File.separator + "python3";
            PIP_EXEC = VENV_DIR_NAME + File.separator + "bin" + File.separator + "pip";
            SYS_PYTHON = "python3";
        }
    }

    public static void main(String[] args) {
        importExtractedFaces();
    }

    public static void importExtractedFaces() {
        if (!setupVirtualEnv()) {
            System.err.println("CRITICAL: Failed to setup Python environment.");
            return;
        }
        
        // FIX 1: Force HTTP/1.1 to prevent "Invalid HTTP request" (Error 400)
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        
        // 1. Resolve Collection Name to ID
        String collectionId = getCollectionId(client, TARGET_COLLECTION_NAME);
        if (collectionId == null) {
            System.err.println("CRITICAL: Collection '" + TARGET_COLLECTION_NAME + "' not found. Please run ChromaCollectionSetup first.");
            return;
        }

        // 2. CHECK: If data exists, stop to avoid duplication
        if (doesCollectionHaveObjects(client, collectionId)) {
            System.out.println("INFO: Collection already contains data. Skipping import.");
            return;
        }

        // 3. Import
        importPickleData(client, collectionId);
    }

    private static void importPickleData(HttpClient client, String collectionId) {
        String chromaAddUrl = CHROMA_BASE_URL + "/collections/" + collectionId + "/add";

        if (!new File(PICKLE_FILE).exists()) {
            System.err.println("Error: Pickle file not found: " + new File(PICKLE_FILE).getAbsolutePath());
            return;
        }

        File tempScript = createPythonWorkerScript();
        if (tempScript == null) return;

        try {
            System.out.println("Starting Python process to read pickle...");
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, tempScript.getAbsolutePath(), PICKLE_FILE);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ERROR")) {
                    System.err.println("Python Report: " + line);
                    continue;
                }
                
                if (line.startsWith("DEBUG")) {
                    System.out.println("Python Debug: " + line);
                    continue;
                }

                String[] parts = line.split("\\|", 4);
                if (parts.length != 4) {
                    continue;
                }

                String uuid = parts[0];
                String vectorJson = parts[1];
                String imageName = parts[2];
                String filePath = parts[3];

                sendToChroma(client, chromaAddUrl, uuid, vectorJson, imageName, filePath);
                
                count++;
                if (count % 100 == 0) {
                    System.out.print("\rImported: " + count + " extracted faces...");
                }
            }

            while ((line = errorReader.readLine()) != null) {
                System.err.println("PYTHON ERR: " + line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nProcess finished with exit code: " + exitCode);
            tempScript.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getCollectionId(HttpClient client, String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHROMA_BASE_URL + "/collections/" + name))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"id\":\"([a-f0-9\\-]+)\"");
                java.util.regex.Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching collection ID: " + e.getMessage());
        }
        return null;
    }

    private static boolean doesCollectionHaveObjects(HttpClient client, String collectionId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHROMA_BASE_URL + "/collections/" + collectionId + "/count"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                int count = Integer.parseInt(response.body().trim());
                System.out.println("INFO: Existing count for collection is " + count);
                return count > 0;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check object count: " + e.getMessage());
        }
        return false;
    }

    // FIX 2: Better String Escaper to avoid 422 Errors on paths with backslashes
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")  // Escape backslashes first
                    .replace("\"", "\\\"")  // Escape quotes
                    .replace("\n", "\\n")   // Escape newlines
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static void sendToChroma(HttpClient client, String url, String uuid, String vector, String imageName, String filePath) {
        
        // Use the safe escaper
        String safeImageName = escapeJson(imageName);
        String safePath = escapeJson(filePath);

        // Construct JSON manually but safely
        String jsonPayload = String.format(
            "{" +
            "  \"ids\": [\"%s\"]," +
            "  \"embeddings\": [%s]," +
            "  \"metadatas\": [{" +
            "    \"imageName\": \"%s\"," +
            "    \"filePath\": \"%s\"" +
            "  }]" +
            "}",
            uuid, vector, safeImageName, safePath
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                System.err.println("\nAPI Error " + response.statusCode() + " for " + imageName + ": " + response.body());
                // Print the payload causing issues so we can debug
                System.err.println("Failed Payload snippet: " + jsonPayload.substring(0, Math.min(jsonPayload.length(), 200)) + "...");
            }
        } catch (Exception e) {
            System.err.println("Exception sending " + imageName + ": " + e.getMessage());
        }
    }

    private static File createPythonWorkerScript() {
        try {
            // Script logic remains the same
            String scriptContent = """
            import pickle
            import json
            import uuid
            import sys
            import os
            import numpy as np

            pickle_file = sys.argv[1]

            if not os.path.exists(pickle_file):
                print(f"ERROR|File not found: {pickle_file}")
                sys.exit(1)

            try:
                with open(pickle_file, 'rb') as f:
                    data = pickle.load(f)
                
                print(f"DEBUG|Loaded {len(data)} records")

                for item in data:
                    try:
                        raw_label = item.get('label', 'Unknown')
                        path = item.get('path', '')
                        vec_raw = item.get('vector')
                        
                        if hasattr(vec_raw, 'tolist'):
                            vec_list = vec_raw.tolist()
                        else:
                            vec_list = vec_raw
                        
                        vec_json = json.dumps(vec_list)
                        
                        # Use a namespace UUID based on path to ensure consistency
                        unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, str(path)))

                        # Sanitize output by replacing potential pipe characters in data
                        safe_label = str(raw_label).replace('|', '')
                        safe_path = str(path).replace('|', '')

                        print(f"{unique_id}|{vec_json}|{safe_label}|{safe_path}")
                        sys.stdout.flush()

                    except Exception as inner_e:
                        print(f"ERROR|Processing item: {str(inner_e)}")

            except Exception as e:
                print(f"ERROR|Global script error: {str(e)}")
                sys.exit(1)
            """;
            
            File temp = File.createTempFile("pickle_face_worker", ".py");
            try (FileWriter writer = new FileWriter(temp)) {
                writer.write(scriptContent);
            }
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean setupVirtualEnv() {
        File venvDir = new File(VENV_DIR_NAME);
        if (!venvDir.exists()) {
            if (!runCommand(SYS_PYTHON, "-m", "venv", VENV_DIR_NAME)) return false;
        }
        if (!runCommand(PIP_EXEC, "install", "numpy", "--quiet")) return false;
        return true;
    }

    private static boolean runCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}