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

import cat.dog.utility.DatabaseConfig;

public class MemeVectorImporter {

    private static final String VENV_DIR_NAME = ".venv";
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
        importVectors("MemeImageCleaned", "./../../DATA/embeddings_cleaned/");
        // You can call it again for another class/dir:
        // importVectors("OtherClass", "./../../DATA/other_embeddings/");
    }

    public static void importVectors(String className, String embeddingsDir) {
        if (!setupVirtualEnv()) {
            System.err.println("CRITICAL: Failed to setup local Python virtual environment. Exiting.");
            System.exit(1);
        }

        HttpClient client = HttpClient.newHttpClient();

        if (doesClassHaveObjects(client, className)) {
            System.out.println("INFO: Class '" + className + "' already contains vectors. Skipping import.");
            return;
        }

        Path startPath = Paths.get(embeddingsDir);

        if (!Files.exists(startPath)) {
            System.err.println("Error: Embeddings directory not found at " + startPath.toAbsolutePath());
            return;
        }

        try (Stream<Path> stream = Files.walk(startPath)) {
            List<Path> files = stream
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.toString().endsWith(".npy"))
                    .collect(Collectors.toList());

            int total = files.size();
            System.out.println("Found " + total + " vector files for class " + className);

            AtomicInteger counter = new AtomicInteger(0);
            for (Path file : files) {
                processFile(client, file, counter.incrementAndGet(), total, className);
            }

            System.out.println("\nImport process complete for class " + className);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean doesClassHaveObjects(HttpClient client, String className) {
        try {
            String graphqlUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";
            
            // 1. Construct the GraphQL query payload
            // query: "{ Aggregate { YourClassName { meta { count } } } }"
            String query = String.format("{ Aggregate { %s { meta { count } } } }", className);
            
            // 2. Wrap it in a JSON object: { "query": "..." }
            String jsonPayload = String.format("{\"query\": \"%s\"}", query);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphqlUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                
                // 3. Extract the count using Regex
                // Matches: "count": 6991 (ignoring whitespace)
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"count\"\\s*:\\s*(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(body);
                
                if (matcher.find()) {
                    int count = Integer.parseInt(matcher.group(1));
                    System.out.println("INFO: Existing count for class '" + className + "' is " + count);
                    return count > 0;
                }
            } else {
                 // If the class doesn't exist, Weaviate might return a 200 with errors in the body
                 // or a different status code depending on version.
                 // If we can't get a 200 OK with a count, we assume it's safe to try importing.
                 System.out.println("DEBUG: Failed to get count (Status " + response.statusCode() + "). Body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not check object count. Proceeding with caution. Error: " + e.getMessage());
        }
        return false;
    }

    private static boolean setupVirtualEnv() {
        File venvDir = new File(VENV_DIR_NAME);

        if (!venvDir.exists()) {
            System.out.println("Creating local virtual environment in ./" + VENV_DIR_NAME + " ...");
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

    private static void processFile(HttpClient client, Path filePath, int current, int total, String className) {
        String WEAVIATE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/objects";
        String fileName = filePath.getFileName().toString();
        String absPath = filePath.toAbsolutePath().toString()
                .replace("\\", "\\\\")
                .replace("'", "\\'");

        if (current % 50 == 0 || current == total) {
            System.out.printf("Progress: %d/%d (%.1f%%) - Processing %s%n",
                    current, total, (current / (float) total) * 100, fileName);
        }

        try {
            String pythonScript =
                    "import numpy as np, json, uuid\n" +
                    "try:\n" +
                    "  vector = np.load('" + absPath + "')\n" +
                    "  vec_json = json.dumps(vector.flatten().tolist())\n" +
                    "  unique_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, '" + fileName + "'))\n" +
                    "  print(f'{unique_id}|{vec_json}')\n" +
                    "except Exception as e:\n" +
                    "  print(f'ERROR|{str(e)}')";

            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, "-c", pythonScript);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();

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
                "{\n" +
                "  \"id\": \"%s\",\n" +
                "  \"class\": \"%s\",\n" +
                "  \"vector\": %s,\n" +
                "  \"properties\": {\n" +
                "    \"name\": \"%s\"\n" +
                "  }\n" +
                "}",
                uuid,
                className,
                vectorJson,
                fileName
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
