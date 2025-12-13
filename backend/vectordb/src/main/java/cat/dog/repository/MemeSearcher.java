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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

import cat.dog.utility.ClipEmbedder;
import cat.dog.utility.DatabaseConfig;

public class MemeSearcher {
    
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
        String classString = "MemeImage";

        searchByImage(queryImagePath, classString, null);
        // String textQuery = "A funny minion meme";
        // searchByText(textQuery, classString, null);
    }
    public static List<String> searchByText(String textQuery, String classString, Map<String, String> filters) {
        String vectorString = ClipEmbedder.embedText(textQuery);

        if (vectorString == null) {
            System.err.println("Failed to generate vector.");
            return new ArrayList<>();
        }

        String graphqlQuery = buildWeivateQuery(vectorString, classString, filters);

        return executeSearch(graphqlQuery, classString);
    }

    

    public static List<String> searchByImage(String imagePath, String classString, Map<String, String> filters) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("Error: Image file does not exist");
            return new ArrayList<>();
        }

        String vectorString = ClipEmbedder.embedImage(imagePath);

        if (vectorString == null) {
            System.err.println("Failed to generate vector.");
            return new ArrayList<>();
        }

        String graphqlQuery = buildWeivateQuery(vectorString, classString, filters);

       return executeSearch(graphqlQuery, classString);
    }
    

    private static String buildWeivateQuery(String vectorJson, String classString, Map<String, String> filters) {
        // TO DO: add filters to the query
        String filterString = "";
        if (filters != null && !filters.isEmpty()) {
            filterString = buildFilters(filters);
        }
        
        return String.format(
            "{ \"query\": \"{ Get { %s(nearVector: {vector: %s} limit: 20 %s) { name _additional { distance } } } }\" }",
            classString,
            vectorJson,
            filterString
        );

    }

    private static String buildFilters(Map<String,String> filters) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'buildFilters'");
    }
    
    private static List<String> executeSearch(String jsonPayload, String classString) {
        final String WEVIATE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";
        List<String> imageNames = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEVIATE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            String responseBody = response.body();
            // System.out.println(formatJson(responseBody));

            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                JSONArray items = obj.getJSONObject("data")
                                    .getJSONObject("Get")
                                    .getJSONArray(classString);

                for (int i = 0; i < items.length(); i++) {
                    String name = items.getJSONObject(i).getString("name");
                    if (name.endsWith(".npy")) {
                        name = name.substring(0, name.length() - 4); // remove .npy
                    }
                    imageNames.add(name);
                }
            } else {
                System.err.println("Error " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageNames;
    }
    private static String formatJson(String input) {
        return input.replaceAll(",", ",\n").replaceAll("\\{", "{\n").replaceAll("}", "\n}");
    }

}
