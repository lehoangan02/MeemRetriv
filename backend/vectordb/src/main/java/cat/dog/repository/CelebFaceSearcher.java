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

import cat.dog.dto.CelebEmbedding;
import cat.dog.utility.ClipEmbedder;
import cat.dog.utility.DatabaseConfig;

public class CelebFaceSearcher {
    
    // Matches the class name in your WeviateSchema
    private static final String CLASS_NAME = "CelebFaceEmbeddings";

    public static void main(String[] args) {
        String queryImagePath = "./received_images/bill_gates.jpg";
        
        System.out.println("Searching for similar celebrity faces...");
        
        // Return type is now List<CelebEmbedding>
        List<CelebEmbedding> results = searchByImage(queryImagePath, null);
        
        for (CelebEmbedding result : results) {
            System.out.println("Found: " + result.getCelebName() + " | Path: " + result.getImagePath());
        }
        // String celebName = "Bill_Gates";
        // List<CelebEmbedding> results = getEmbeddingsByName(celebName);
        // System.out.println("Search results for celebrity name: " + celebName);
        // for (CelebEmbedding result : results) {
        //     System.out.println("Found: " + result.getCelebName() + " | Path: " + result.getImagePath());
        // }   

    }
    public static List<CelebEmbedding> getEmbeddingsByName(String celebName) {
        // Weaviate GraphQL 'where' filter for exact string matching
        // Note the triple backslashes (\\\"%s\\\") needed to escape quotes inside the JSON query string
        String graphqlQuery = String.format(
            "{ \"query\": \"{ Get { %s(where: {path: [\\\"name\\\"], operator: Equal, valueString: \\\"%s\\\"}) { name filePath _additional { vector } } } }\" }",
            CLASS_NAME,
            celebName
        );

        return executeSearch(graphqlQuery);
    }
    public static List<CelebEmbedding> searchByImage(String imagePath, Map<String, String> filters) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("Error: Image file does not exist at " + imagePath);
            return new ArrayList<>();
        }

        // Using the existing ClipEmbedder
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
        
        // UPDATED: Added 'vector' to the _additional block to fetch the embedding
        return String.format(
            "{ \"query\": \"{ Get { %s(nearVector: {vector: %s} limit: 10 %s) { name filePath _additional { distance vector } } } }\" }",
            CLASS_NAME,
            vectorJson,
            filterString
        );
    }

    private static String buildFilters(Map<String,String> filters) {
        // Placeholder for future filter logic
        throw new UnsupportedOperationException("Unimplemented method 'buildFilters'");
    }
    
    private static List<CelebEmbedding> executeSearch(String jsonPayload) {
        final String WEAVIATE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";
        List<CelebEmbedding> results = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEAVIATE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // System.out.println(formatJson(response.body())); // Uncomment for debugging

            if (response.statusCode() == 200) {
                JSONObject obj = new JSONObject(response.body());
                
                // Navigate to: data -> Get -> CelebFaceEmbeddings
                if (obj.has("data") && !obj.isNull("data")) {
                    JSONObject data = obj.getJSONObject("data");
                    if (data.has("Get") && !data.isNull("Get")) {
                        JSONArray items = data.getJSONObject("Get").optJSONArray(CLASS_NAME);

                        if (items != null) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                
                                String name = item.optString("name", "Unknown");
                                String path = item.optString("filePath", "");

                                // Extract the vector from _additional
                                String[] embeddingArray = new String[0];
                                if (item.has("_additional")) {
                                    JSONObject additional = item.getJSONObject("_additional");
                                    if (additional.has("vector")) {
                                        JSONArray vectorJson = additional.getJSONArray("vector");
                                        embeddingArray = new String[vectorJson.length()];
                                        for (int j = 0; j < vectorJson.length(); j++) {
                                            // Convert float/double to String to match DTO
                                            embeddingArray[j] = String.valueOf(vectorJson.get(j));
                                        }
                                    }
                                }

                                // Create DTO and add to list
                                results.add(new CelebEmbedding(name, path, embeddingArray));
                            }
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