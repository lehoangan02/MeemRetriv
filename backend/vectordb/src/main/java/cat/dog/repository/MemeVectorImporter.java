package cat.dog.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MemeVectorImporter {

    // Configuration
    private static final String WEAVIATE_URL = "http://127.0.0.1:8080/v1/objects";
    private static final String EMBEDDINGS_DIR = "./../../DATA/embeddings/"; 
    private static final String CLASS_NAME = "MemeImage";
    
    // Virtual Environment Configuration
    private static final String VENV_DIR_NAME = ".venv";
    private static final String PYTHON_EXEC;
    private static final String PIP_EXEC;

    // Static block to determine OS-specific paths for Python/Pip inside venv
    static {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "Scripts" + File.separator + "python.exe";
            PIP_EXEC = VENV_DIR_NAME + File.separator + "Scripts" + File.separator + "pip.exe";
        } else {
            // MacOS / Linux
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "bin" + File.separator + "python3";
            PIP_EXEC = VENV_DIR_NAME + File.separator + "bin" + File.separator + "pip";
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
            stream
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> p.toString().endsWith(".npy"))
                .forEach(path -> processFile(client, path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a local .venv and installs numpy into it.
     */
    private static boolean setupVirtualEnv() {
        File venvDir = new File(VENV_DIR_NAME);
        
        // 1. Create venv if it doesn't exist
        if (!venvDir.exists()) {
            System.out.println("Creating local virtual environment in ./" + VENV_DIR_NAME + " ...");
            if (!runCommand("python3", "-m", "venv", VENV_DIR_NAME)) {
                System.err.println("Failed to create venv.");
                return false;
            }
        }

        // 2. Install numpy into the venv
        System.out.println("Installing numpy into local venv...");
        // We use the PIP inside the venv to ensure isolation
        if (!runCommand(PIP_EXEC, "install", "numpy")) {
            System.err.println("Failed to install numpy.");
            return false;
        }

        System.out.println("Virtual environment ready.");
        return true;
    }

    /**
     * Helper to run a shell command and check exit code
     */
    private static boolean runCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Show output in console
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void processFile(HttpClient client, Path filePath) {
        String fileName = filePath.getFileName().toString();
        String absPath = filePath.toAbsolutePath().toString();

        System.out.println("Processing: " + fileName);

        try {
            // 3. Get Vector and UUID using the VENV Python
            // UPDATED: Using \n for newlines to avoid SyntaxError in Python
            String pythonScript = 
                "import numpy as np, json, uuid\n" +
                "try:\n" +
                "  vector = np.load('" + absPath + "')\n" +
                "  vec_json = json.dumps(vector.flatten().tolist())\n" +
                "  unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, '" + fileName + "'))\n" +
                "  print(f'{unique_id}|{vec_json}')\n" +
                "except Exception as e:\n" +
                "  print(f'ERROR|{str(e)}')";

            // Use the isolated python executable
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, "-c", pythonScript);
            Process process = pb.start();

            // Read Stdout
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            
            // Read Stderr (CRITICAL: This allows us to see why it fails)
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorLog = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorLog.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();

            if (exitCode != 0 || output == null || output.startsWith("ERROR|")) {
                String err = (output != null && output.startsWith("ERROR|")) ? output.substring(6) : errorLog.toString();
                System.err.println("Failed to read .npy file: " + fileName);
                System.err.println("   Reason: " + (err.isEmpty() ? "Unknown (Process exit code " + exitCode + ")" : err));
                return;
            }

            // 4. Parse Output (Format: UUID|JSON)
            String[] parts = output.split("\\|", 2);
            String uuid = parts[0];
            String vectorJson = parts[1];

            // 5. Construct JSON Payload
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

            // 6. Send Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEAVIATE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 7. Handle Response
            if (response.statusCode() == 200) {
                System.out.println("SUCCESS: " + fileName + " (ID: " + uuid + ")");
            } else if (response.statusCode() == 422 || response.statusCode() == 500) {
                if (response.body().contains("already exists")) {
                    System.out.println("SKIPPED: " + fileName + " (Already exists)");
                } else {
                    System.err.println("ERROR " + response.statusCode() + ": " + response.body());
                }
            } else {
                System.err.println("ERROR " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Exception processing " + fileName + ": " + e.getMessage());
        }
    }
}