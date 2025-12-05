package cat.dog.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemeVectorImporter {

    // Configuration
    private static final String WEAVIATE_URL = "http://127.0.0.1:8080/v1/objects";
    // Adjust this path if your embeddings are elsewhere relative to where you run the command
    private static final String EMBEDDINGS_DIR = "./../../DATA/embeddings/"; 
    private static final String CLASS_NAME = "MemeImage";
    
    // Virtual Environment Configuration
    private static final String VENV_DIR_NAME = ".venv";
    private static final String PYTHON_EXEC;
    private static final String PIP_EXEC;
    private static final String SYS_PYTHON; // System python command (python vs python3)

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
        // 1. Setup Isolated Python Environment
        if (!setupVirtualEnv()) {
            System.err.println("CRITICAL: Failed to setup local Python virtual environment. Exiting.");
            System.exit(1);
        }

        HttpClient client = HttpClient.newHttpClient();
        Path startPath = Paths.get(EMBEDDINGS_DIR);

        System.out.println("Starting import from: " + startPath.toAbsolutePath());

        if (!Files.exists(startPath)) {
            System.err.println("Error: Embeddings directory not found at " + startPath.toAbsolutePath());
            return;
        }

        try (Stream<Path> stream = Files.walk(startPath)) {
            // Collect to list first to get total count for progress bar
            List<Path> files = stream
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> p.toString().endsWith(".npy"))
                .collect(Collectors.toList());
            
            int total = files.size();
            System.out.println("Found " + total + " vector files. This will take some time...");
            
            AtomicInteger counter = new AtomicInteger(0);

            files.forEach(path -> {
                int current = counter.incrementAndGet();
                processFile(client, path, current, total);
            });
            
            System.out.println("\nImport process complete!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean setupVirtualEnv() {
        File venvDir = new File(VENV_DIR_NAME);
        
        if (!venvDir.exists()) {
            System.out.println("Creating local virtual environment in ./" + VENV_DIR_NAME + " ...");
            // Fix: Use SYS_PYTHON instead of hardcoded 'python3' for Windows support
            if (!runCommand(SYS_PYTHON, "-m", "venv", VENV_DIR_NAME)) {
                System.err.println("Failed to create venv.");
                return false;
            }
        }

        System.out.println("Checking numpy installation...");
        if (!runCommand(PIP_EXEC, "install", "numpy", "--quiet")) {
            System.err.println("Failed to install numpy.");
            return false;
        }

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

    private static void processFile(HttpClient client, Path filePath, int current, int total) {
        String fileName = filePath.getFileName().toString();
        // Fix 1: Properly handle Windows paths and escape single quotes in filenames
        String absPath = filePath.toAbsolutePath().toString()
                                 .replace("\\", "\\\\")
                                 .replace("'", "\\'");

        // Simple progress log every 50 files
        if (current % 50 == 0 || current == total) {
            System.out.printf("Progress: %d / %d (%.1f%%) - Processing %s\n", 
                              current, total, (current / (float)total) * 100, fileName);
        }

        try {
            String pythonScript = 
                "import numpy as np, json, uuid\n" +
                "try:\n" +
                "  vector = np.load('" + absPath + "')\n" +
                "  vec_json = json.dumps(vector.flatten().tolist())\n" +
                "  # Deterministic UUID based on filename ensures no duplicates on re-run\n" +
                "  unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, '" + fileName + "'))\n" +
                "  print(f'{unique_id}|{vec_json}')\n" +
                "except Exception as e:\n" +
                "  print(f'ERROR|{str(e)}')";

            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, "-c", pythonScript);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            
            // Consume stderr to prevent blocking if buffer fills up
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorLog = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorLog.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();

            if (exitCode != 0 || output == null || output.startsWith("ERROR|")) {
                String err = (output != null && output.startsWith("ERROR|")) ? output.substring(6) : errorLog.toString();
                System.err.println("[FAIL] " + fileName + " : " + err.trim());
                return;
            }

            String[] parts = output.split("\\|", 2);
            String uuid = parts[0];
            String vectorJson = parts[1];

            String jsonPayload = String.format(
                "{" +
                    "\"id\": \"%s\"," +
                    "\"class\": \"%s\"," +
                    "\"vector\": %s," +
                    "\"properties\": {" +
                        "\"name\": \"%s\"" +
                    "}" +
                "}", 
                uuid, CLASS_NAME, vectorJson, fileName
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEAVIATE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // If it's not a 422 (duplicate), print error
                if (response.statusCode() == 422 && response.body().contains("already exists")) {
                    // Silently skip duplicates
                } else {
                    System.err.println("API Error " + response.statusCode() + " for " + fileName + ": " + response.body());
                }
            }

        } catch (Exception e) {
            System.err.println("Exception processing " + fileName + ": " + e.getMessage());
        }
    }
}