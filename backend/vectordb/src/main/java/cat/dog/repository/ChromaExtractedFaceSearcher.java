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

public class ChromaExtractedFaceSearcher {

    // UPDATE: Matches the collection name you successfully created and imported into
    private static final String COLLECTION_NAME = "ExtractedFaceEmbeddings";
    
    // Port 8001 as per your Docker setup
    private static final String CHROMA_BASE_URL = "http://localhost:8001/api/v1";

    /**
     * Standard search: Takes an image path -> generates embedding -> searches DB.
     */
    public static List<MemeFaceRecord> searchFace(String filePath, int limit) {
        File imgFile = new File(filePath);
        if (!imgFile.exists()) {
            System.err.println("Error: File does not exist -> " + filePath);
            return new ArrayList<>();
        }

        // 1. Get Embedding (Slow step)
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

        // 2. Search Chroma
        return searchChroma(vectorStr, limit);
    }

    public static List<MemeFaceRecord> searchFaceWithEmbedding(String[] vector, int limit) {
        if (vector == null || vector.length == 0) {
            return new ArrayList<>();
        }
        // Join the array elements with commas and wrap in brackets to make valid JSON
        String vectorJson = "[" + String.join(", ", vector) + "]";
        
        return searchChroma(vectorJson, limit);
    }

    /**
     * NEW METHOD OVERLOAD: Search using a Java List of Floats.
     * Automatically converts the list to the required JSON format.
     */
    public static List<MemeFaceRecord> searchFaceWithEmbedding(List<Float> embedding, int limit) {
        if (embedding == null || embedding.isEmpty()) {
            return new ArrayList<>();
        }
        // Java's default toString for Lists produces valid JSON arrays: "[1.0, 2.0, 3.0]"
        return searchChroma(embedding.toString(), limit);
    }

    // --- Internal Worker Methods ---

    private static List<MemeFaceRecord> searchChroma(String vectorStr, int limit) {
        List<MemeFaceRecord> results = new ArrayList<>();
        
        // Force HTTP/1.1
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // A. Get Collection ID
        String collectionId = getCollectionId(client, COLLECTION_NAME);
        if (collectionId == null) {
            System.err.println("Error: Collection '" + COLLECTION_NAME + "' not found.");
            return results;
        }

        try {
            String queryUrl = CHROMA_BASE_URL + "/collections/" + collectionId + "/query";

            // B. Construct Payload
            // Chroma requires a list of lists: [[0.1, ...]]
            String jsonPayload = String.format(
                "{" +
                "  \"query_embeddings\": [%s]," + 
                "  \"n_results\": %d" +
                "}",
                vectorStr, limit
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                results = parseChromaResponse(response.body());
            } else {
                System.err.println("Chroma Error: " + response.statusCode());
                System.err.println("Body: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private static String getCollectionId(HttpClient client, String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHROMA_BASE_URL + "/collections/" + name))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                Pattern pattern = Pattern.compile("\"id\":\"([a-f0-9\\-]+)\"");
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<MemeFaceRecord> parseChromaResponse(String jsonResponse) {
        List<MemeFaceRecord> records = new ArrayList<>();
        
        // Extract "metadatas": [[ ... ]] block
        Pattern metadataSectionPattern = Pattern.compile("\"metadatas\"\\s*:\\s*\\[\\s*\\[(.*?)\\]\\s*\\]");
        Matcher sectionMatcher = metadataSectionPattern.matcher(jsonResponse);

        if (sectionMatcher.find()) {
            String innerContent = sectionMatcher.group(1);
            
            // Find individual objects inside
            Pattern objectPattern = Pattern.compile("\\{[^{}]*?\"imageName\"[^{}]*?\\}");
            Matcher objectMatcher = objectPattern.matcher(innerContent);

            while (objectMatcher.find()) {
                String objectJson = objectMatcher.group();
                String name = extractValue(objectJson, "imageName");
                String path = extractValue(objectJson, "filePath");
                
                if (name != null && path != null) {
                    records.add(new MemeFaceRecord(name, path));
                }
            }
        }
        return records;
    }

    private static String extractValue(String jsonSnippet, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(jsonSnippet);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    // --- Usage Example ---
    public static void main(String[] args) {
        // Example 1: Search by File (Generates embedding)
        String testPath = "./received_images/leonardo_di_caprio.jpg"; 
        System.out.println("--- Search by File ---");
        List<MemeFaceRecord> fileResults = searchFace(testPath, 3);
        printResults(fileResults);

        // Example 2: Search by Pre-loaded Embedding (Mock data)
        // This simulates having the vector already loaded in memory
        System.out.println("\n--- Search by Pre-loaded Embedding ---");
        
        // Imagine this list came from a cache or another service
        // (Just a dummy vector for demonstration, length must match your model, e.g. 512)
        // String dummyVector = "[0.01, 0.02, 0.03 ...]"; 
        // List<MemeFaceRecord> directResults = searchFaceWithEmbedding(dummyVector, 3);
        // printResults(directResults);
    }
    
    private static void printResults(List<MemeFaceRecord> results) {
        if (results.isEmpty()) {
            System.out.println("No matches found.");
        }
        for (MemeFaceRecord rec : results) {
            System.out.println("Match: " + rec.getMemeName() + " | " + rec.getFilePath());
        }
    }
}