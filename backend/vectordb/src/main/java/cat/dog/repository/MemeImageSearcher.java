package cat.dog.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.util.Map;

public class MemeImageSearcher {
    
    private static final String WEVIATE_URL = "http://127.0.0.1:8080/v1/graphql";
    private static final String VENV_DIR_NAME = ".venv";
    private static final String PYTHON_EXEC;

    // set up python virtual environment activation path
    static {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows)
        {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "Scripts" + File.separator + "python.exe";
        } else {
            PYTHON_EXEC = VENV_DIR_NAME + File.separator + "bin" + File.separator + "python3";
        }
        
    }

    public static void main(String[] args) {
        String queryImagePath = "./meme_minion.jpg";

        searchByImage(queryImagePath, null);
    }

    private static void searchByImage(String imagePath, Map<String, String> filters) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("Error: Image file does not exist");
            return;
        }

        String vectorString = getVectorMobileClip(imagePath);

        if (vectorString == null) {
            System.err.println("Failed to generate vector.");
            return;
        }

        String graphqlQuery = buildWeivateQuery(vectorString, filters);

        executeSearch(graphqlQuery);
    }
    private static String getVectorMobileClip(String absPath) {
        String pythonScript = 
            "import torch, mobileclip, json\n" +
            "from PIL import Image\n" +
            "import numpy as np\n" +
            "try:\n" +
            "    model, _, preprocess = mobileclip.create_model_and_transforms('mobileclip_s0', pretrained='mobileclip_s0.pt')\n" +
            "    image = Image.open('" + absPath.replace("\\", "\\\\") + "').convert('RGB')\n" +
            "    image_tensor = preprocess(image).unsqueeze(0)\n" +
            "    with torch.no_grad():\n" +
            "        features = model.encode_image(image_tensor)\n" +
            "        # Normalize the vector (important for cosine similarity)\n" +
            "        features /= features.norm(dim=-1, keepdim=True)\n" +
            "    print('VECTOR_START|' + json.dumps(features.cpu().numpy().flatten().tolist()))\n" +
            "except Exception as e:\n" +
            "    print(f'ERROR|{e}')";
        try {
            
            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXEC, "-c", pythonScript);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new BufferedReader(new InputStreamReader(process.getInputStream())));
            String line;
            while ((line = reader.readLine()) != null) {
                // System.out.println("PYTHON: " + line); uncomment when debugging uncommon python errors
                if (line.startsWith("VECTOR_START|")) {
                    
                    return line.split("\\|")[1];
                } else if (line.startsWith("ERROR|")) {
                    System.err.println("Python Error:");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;    
    }

    private static String buildWeivateQuery(String vectorJson, Map<String, String> filters) {
        // TO DO: add filters to the query
        String filterString = "";
        if (filters != null && !filters.isEmpty()) {
            filterString = buildFilters(filters);
        }
        
        return String.format(
            "{ \"query\": \"{ Get { MemeImage(nearVector: {vector: %s} limit: 3 %s) { name _additional { distance } } } }\" }",
            vectorJson,
            filterString
        );

    }

    private static String buildFilters(Map<String,String> filters) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'buildFilters'");
    }
    
    private static void executeSearch(String jsonPayload) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEVIATE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // In a real app, use a JSON library (Jackson/Gson) to parse this.
                // For learning, we just print the raw JSON response.
                System.out.println(formatJson(response.body()));
            } else {
                System.err.println("Error " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String formatJson(String input) {
        return input.replaceAll(",", ",\n").replaceAll("\\{", "{\n").replaceAll("}", "\n}");
    }

}
