package cat.dog.repository;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cat.dog.dto.MemeFaceRecord;
import cat.dog.utility.ClipEmbedder;
import cat.dog.utility.DatabaseConfig;

public class ExtractedFaceSearcher {

    private static final String CLASS_NAME = "ExtractedFaceEmbeddings";

    public List<MemeFaceRecord> searchFace(String filePath, int limit) {
        File imgFile = new File(filePath);
        if (!imgFile.exists()) {
            System.err.println("Error: File does not exist -> " + filePath);
            return new ArrayList<>();
        }

        // 1. Get Embedding
        String vectorStr = null;
        try {
            vectorStr = ClipEmbedder.embedImage(imgFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error generating embedding: " + e.getMessage());
            return new ArrayList<>();
        }

        if (vectorStr == null || vectorStr.isEmpty() || vectorStr.equals("[]")) {
            System.err.println("Error: Embedder returned empty vector.");
            return new ArrayList<>();
        }

        // DEBUG: Confirm vector generation worked
        // System.out.println("DEBUG: Vector generated successfully (length: " + vectorStr.length() + ")");

        // 2. Search Weaviate
        return searchWeaviate(vectorStr, limit);
    }

    private List<MemeFaceRecord> searchWeaviate(String vectorStr, int limit) {
        List<MemeFaceRecord> results = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String graphqlUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/graphql";

            String query = String.format(
                "{ Get { %s ( nearVector: { vector: %s } limit: %d ) { imageName filePath } } }",
                CLASS_NAME, vectorStr, limit
            );

            String jsonPayload = "{\"query\": \"" + query.replace("\"", "\\\"") + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(graphqlUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // DEBUG: Print raw response to verify data exists and field order
                System.out.println("DEBUG: Raw Response: " + response.body());
                results = parseResponse(response.body());
            } else {
                System.err.println("Weaviate Error: " + response.statusCode());
                System.err.println("Body: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private List<MemeFaceRecord> parseResponse(String jsonResponse) {
        List<MemeFaceRecord> records = new ArrayList<>();
        
        // Improved Parsing: 
        // 1. Find individual object blocks {...} inside the response
        // 2. Extract fields from each block independently (order doesn't matter)
        
        // Regex to find JSON objects containing our specific fields
        // This looks for `{ ... "imageName" ... }` non-greedily
        Pattern objectPattern = Pattern.compile("\\{[^{}]*?\"imageName\"[^{}]*?\\}");
        Matcher objectMatcher = objectPattern.matcher(jsonResponse);

        while (objectMatcher.find()) {
            String objectJson = objectMatcher.group();
            
            String name = extractValue(objectJson, "imageName");
            String path = extractValue(objectJson, "filePath");
            
            if (name != null && path != null) {
                records.add(new MemeFaceRecord(name, path));
            }
        }
        return records;
    }

    // Helper to extract a single value by key from a JSON snippet
    private String extractValue(String jsonSnippet, String key) {
        // Matches: "key" : "value"
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(jsonSnippet);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static void main(String[] args) {
        ExtractedFaceSearcher searcher = new ExtractedFaceSearcher();
        String testPath = "./received_images/leonardo_di_caprio.jpg"; 
        
        System.out.println("Searching for faces matching: " + testPath);
        List<MemeFaceRecord> results = searcher.searchFace(testPath, 5);
        
        System.out.println("Found " + results.size() + " matches.");
        for (MemeFaceRecord rec : results) {
            System.out.println("Match: " + rec.getMemeName() + " | " + rec.getFilePath());
        }
    }
}