package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChromaCollectionSetup {

    // Ensure this port matches your docker-compose (8001)
    private static final String CHROMA_URL = "http://localhost:8001/api/v1/collections";

    public static void createExtractedFaceCollection(String className) throws Exception {
        // FIX: Force HTTP/1.1 to avoid "Invalid HTTP request" error from Uvicorn
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // Minified JSON to ensure safety
        String jsonPayload = String.format(
            "{\"name\":\"%s\", \"metadata\":{\"hnsw:space\":\"cosine\"}}", 
            className
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHROMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Collection '" + className + "' created successfully.");
        } 
        else if (response.statusCode() == 409) {
            System.out.println("Collection '" + className + "' already exists.");
        } 
        else if (response.statusCode() == 500 && response.body().contains("already exists")) {
            System.out.println("Collection '" + className + "' already exists (Legacy check).");
        } 
        else {
            throw new RuntimeException("Failed to create collection. Status: " + response.statusCode() + " Body: " + response.body());
        }
    }

    public static void main(String[] args) throws Exception {
        createExtractedFaceCollection("face_vectors");
    }
}