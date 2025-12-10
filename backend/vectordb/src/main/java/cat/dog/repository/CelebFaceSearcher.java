package cat.dog.repository;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

import cat.dog.utility.ClipEmbedder;
import cat.dog.utility.DatabaseConfig;

public class CelebFaceSearcher {
    
    // Matches the class name in your WeviateSchema
    private static final String CLASS_NAME = "CelebFaceEmbeddings";

    public static void main(String[] args) {
        String queryImagePath = "./received_images/leonardo_di_caprio.jpg";
        
        System.out.println("Searching for similar celebrity faces...");
        List<String> results = searchByImage(queryImagePath, null);
        for (String result : results) {
            System.out.println(result);
        }
    }

    public static List<String> searchByImage(String imagePath, Map<String, String> filters) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("Error: Image file does not exist at " + imagePath);
            return new ArrayList<>();
        }

        // Using the existing ClipEmbedder as requested
        String vectorString = ClipEmbedder.embedImage(imagePath);

        if (vectorString == null) {
            System.err.println("Failed to generate vector.");
            return new ArrayList<>();
        }

        String graphqlQuery = buildWeaviateQuery(vectorString, filters);

        return executeSearch(graphqlQuery);
    }
    

    private static String buildWeaviateQuery(String vectorJson, Map<String, String> filters) {
        String filterString = "";
        if (filters != null && !filters.isEmpty()) {
            filterString = buildFilters(filters);
        }
        
        // We fetch both 'name' and 'filePath' as per your schema
        return String.format(
            "{ \"query\": \"{ Get { %s(nearVector: {vector: %s} limit: 10 %s) { name filePath _additional { distance } } } }\" }",
            CLASS_NAME,
            vectorJson,
            filterString
        );
    }

    private static String buildFilters(Map<String,String> filters) {
        // Placeholder for future filter logic
        throw new UnsupportedOperationException("Unimplemented method 'buildFilters'");
    }
    
    private static List<String> executeSearch(String jsonPayload) {
        final String WEAVIATE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";
        List<String> results = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEAVIATE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println(formatJson(response.body())); // Uncomment for debugging

            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                
                // Navigate to: data -> Get -> CelebFaceEmbeddings
                if (obj.has("data") && !obj.isNull("data")) {
                    JSONArray items = obj.getJSONObject("data")
                                        .getJSONObject("Get")
                                        .optJSONArray(CLASS_NAME);

                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            
                            String name = item.optString("name", "Unknown");
                            String path = item.optString("filePath", "");
                            
                            // You can format this string however your frontend expects it
                            // e.g., "Brad Pitt|/images/brad.jpg"
                            results.add(name + " : " + path);
                        }
                    }
                }
            } else {
                System.err.println("Error " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    private static String formatJson(String input) {
        return input.replaceAll(",", ",\n").replaceAll("\\{", "{\n").replaceAll("}", "\n}");
    }
}