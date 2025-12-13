package cat.dog.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import cat.dog.utility.DatabaseConfig;

public class WeviateExtractedFaceImporter {

    private static final String VENV_DIR_NAME = ".venv";
    // Adjust path if your pickle file is in a different location relative to the Java root
    private static final String PICKLE_FILE = "./../../DATA/meme_face_embeddings_mobileclip.pkl"; 
    private static final String TARGET_CLASS = "ExtractedFaceEmbeddings";

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
        
        HttpClient client = HttpClient.newHttpClient();
        
        // CHECK: If data exists, stop to avoid duplication.
        if (doesClassHaveObjects(client, TARGET_CLASS)) {
            System.out.println("INFO: Class '" + TARGET_CLASS + "' already contains data. Skipping import.");
            return;
        }

        importPickleData(client);
    }
    private static void importPickleData(HttpClient client) {
        String weaviateUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/objects";

        if (!new File(PICKLE_FILE).exists()) {
            System.err.println("Error: Pickle file not found: " + new File(PICKLE_FILE).getAbsolutePath());
            return;
        }

        // Create temporary python script to parse the specific structure of your meme faces pickle
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

                // Expecting: UUID | VECTOR_JSON | IMAGE_NAME | FILE_PATH
                String[] parts = line.split("\\|", 4);
                if (parts.length != 4) {
                    System.err.println("Skipping malformed line.");
                    continue;
                }

                String uuid = parts[0];
                String vectorJson = parts[1];
                String imageName = parts[2]; // Corresponds to 'label' in pickle
                String filePath = parts[3];  // Corresponds to 'path' in pickle

                sendToWeaviate(client, weaviateUrl, uuid, vectorJson, imageName, filePath);
                
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

    private static boolean doesClassHaveObjects(HttpClient client, String className) {
        try {
            String graphqlUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";
            
            String query = String.format("{ Aggregate { %s { meta { count } } } }", className);
            String jsonPayload = String.format("{\"query\": \"%s\"}", query);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphqlUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"count\"\\s*:\\s*(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(body);
                
                if (matcher.find()) {
                    int count = Integer.parseInt(matcher.group(1));
                    System.out.println("INFO: Existing count for class '" + className + "' is " + count);
                    return count > 0;
                }
            } 
        } catch (Exception e) {
            System.err.println("Warning: Could not check object count. Error: " + e.getMessage());
        }
        return false;
    }

    private static void sendToWeaviate(HttpClient client, String url, String uuid, String vector, String imageName, String filePath) {
        String safeImageName = imageName.replace("\"", "\\\"");
        String safePath = filePath.replace("\"", "\\\"");
        
        // Mapping to ExtractedFaceEmbeddings schema:
        // imageName -> from 'label'
        // filePath  -> from 'path'
        String jsonPayload = String.format(
            "{\n" +
            "  \"id\": \"%s\",\n" +
            "  \"class\": \"%s\",\n" +
            "  \"vector\": %s,\n" +
            "  \"properties\": {\n" +
            "    \"imageName\": \"%s\",\n" +
            "    \"filePath\": \"%s\"\n" +
            "  }\n" +
            "}",
            uuid, TARGET_CLASS, vector, safeImageName, safePath
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 422) { // 422 usually means UUID exists
                System.err.println("API Error " + response.statusCode() + " for " + imageName + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Exception sending " + imageName + ": " + e.getMessage());
        }
    }

    private static File createPythonWorkerScript() {
        try {
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
                        # Parse keys specific to meme_face_embeddings_mobileclip.pkl
                        # Structure: {'label': 'image_x', 'path': '...', 'vector': [...]}
                        
                        raw_label = item.get('label', 'Unknown')
                        path = item.get('path', '')
                        vec_raw = item.get('vector')
                        
                        # Handle vector serialization
                        if hasattr(vec_raw, 'tolist'):
                            vec_list = vec_raw.tolist()
                        else:
                            vec_list = vec_raw
                        
                        vec_json = json.dumps(vec_list)
                        
                        # Create deterministic ID based on the file path so we don't duplicate
                        unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, path))

                        # Output: UUID | VECTOR | IMAGE_NAME | FILE_PATH
                        print(f"{unique_id}|{vec_json}|{raw_label}|{path}")
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
            System.out.println("Creating local virtual environment...");
            if (!runCommand(SYS_PYTHON, "-m", "venv", VENV_DIR_NAME)) return false;
        }

        // Numpy is required for pickle loading if arrays were saved as numpy arrays
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