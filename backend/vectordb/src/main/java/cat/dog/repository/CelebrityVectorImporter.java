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

public class CelebrityVectorImporter {

    private static final String VENV_DIR_NAME = ".venv";
    private static final String PICKLE_FILE = "./../../DATA/celebrity_clip_vectors.pkl"; 
    private static final String TARGET_CLASS = "CelebFaceEmbeddings";

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
        if (!setupVirtualEnv()) {
            System.err.println("CRITICAL: Failed to setup Python environment.");
            return;
        }
        
        // CHECK: If data exists, stop.
        HttpClient client = HttpClient.newHttpClient();
        if (doesClassHaveObjects(client, TARGET_CLASS)) {
            System.out.println("INFO: Class '" + TARGET_CLASS + "' already contains data. Skipping import.");
            return;
        }

        importPickleData(client);
    }

    private static void importPickleData(HttpClient client) {
        String weaviateUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/objects";

        if (!new File(PICKLE_FILE).exists()) {
            System.err.println("Error: Pickle file not found: " + PICKLE_FILE);
            return;
        }

        // We create a temporary python script to run the extraction logic.
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
                    continue;
                }

                String[] parts = line.split("\\|", 4);
                if (parts.length != 4) {
                    System.err.println("Skipping malformed line.");
                    continue;
                }

                String uuid = parts[0];
                String vectorJson = parts[1];
                String name = parts[2];
                String filePath = parts[3];

                sendToWeaviate(client, weaviateUrl, uuid, vectorJson, name, filePath);
                
                count++;
                if (count % 100 == 0) {
                    System.out.print("\rImported: " + count + " celebrities...");
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

    // NEW HELPER METHOD TO CHECK COUNT
    private static boolean doesClassHaveObjects(HttpClient client, String className) {
        try {
            String graphqlUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";
            
            // Query: { Aggregate { CelebrityImage { meta { count } } } }
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

    private static void sendToWeaviate(HttpClient client, String url, String uuid, String vector, String name, String filePath) {
        String safeName = name.replace("\"", "\\\"");
        String safePath = filePath.replace("\"", "\\\"");
        
        String jsonPayload = String.format(
            "{\n" +
            "  \"id\": \"%s\",\n" +
            "  \"class\": \"%s\",\n" +
            "  \"vector\": %s,\n" +
            "  \"properties\": {\n" +
            "    \"name\": \"%s\",\n" +
            "    \"filePath\": \"%s\"\n" +
            "  }\n" +
            "}",
            uuid, TARGET_CLASS, vector, safeName, safePath
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 422) {
                System.err.println("API Error " + response.statusCode() + " for " + name + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Exception sending " + name + ": " + e.getMessage());
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
                        raw_label = item.get('label', 'Unknown')
                        clean_name = raw_label.replace('_', ' ')
                        path = item.get('path', '')
                        vec_raw = item.get('vector')
                        
                        if hasattr(vec_raw, 'tolist'):
                            vec_list = vec_raw.tolist()
                        else:
                            vec_list = vec_raw
                        
                        vec_json = json.dumps(vec_list)
                        unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, path))

                        print(f"{unique_id}|{vec_json}|{clean_name}|{path}")
                        sys.stdout.flush()

                    except Exception as inner_e:
                        print(f"ERROR|Processing item: {str(inner_e)}")

            except Exception as e:
                print(f"ERROR|Global script error: {str(e)}")
                sys.exit(1)
            """;
            
            File temp = File.createTempFile("pickle_worker", ".py");
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