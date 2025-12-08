package cat.dog.repository;

import cat.dog.service.ClipEmbedder;

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

public class MemeSearcher {
    
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
        String queryImagePath = "./received_images/query_image.png";

        searchByImage(queryImagePath, null);
        // String textQuery = "A funny minion meme";
        // searchByText(textQuery, null);
    }
    private static void searchByText(String textQuery, Map<String, String> filters) {
        String vectorString = ClipEmbedder.embedText(textQuery);

        if (vectorString == null) {
            System.err.println("Failed to generate vector.");
            return;
        }

        String graphqlQuery = buildWeivateQuery(vectorString, filters);

        executeSearch(graphqlQuery);
    }

    

    private static void searchByImage(String imagePath, Map<String, String> filters) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("Error: Image file does not exist");
            return;
        }

        String vectorString = ClipEmbedder.embedImage(imagePath);

        if (vectorString == null) {
            System.err.println("Failed to generate vector.");
            return;
        }

        String graphqlQuery = buildWeivateQuery(vectorString, filters);

        executeSearch(graphqlQuery);
    }
    

    private static String buildWeivateQuery(String vectorJson, Map<String, String> filters) {
        // TO DO: add filters to the query
        String filterString = "";
        if (filters != null && !filters.isEmpty()) {
            filterString = buildFilters(filters);
        }
        
        return String.format(
            "{ \"query\": \"{ Get { MemeImage(nearVector: {vector: %s} limit: 20 %s) { name _additional { distance } } } }\" }",
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
