package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeviateSchema {
    private static final String BASE_URL = "http://127.0.0.1:8080/v1/schema";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest getSchemaRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getSchemaRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Schema GET response:");
        System.out.println(getResponse.body());

        boolean classExists = getResponse.body().contains("\"class\":\"MemeImage\"");

        if (classExists) {
            System.out.println("Class 'MemeImage' already exists. Skipping creation.");
            return;
        }
        String schemaJson = """
        {
          "class": "MemeImage",
          "description": "A meme image's precomputed CLIP vector",
          "vectorizer": "none",
          "properties": [
            {"name": "name", "dataType": ["string"]},
            {"name": "vector", "dataType": ["number[]"]}
          ]
        }
        """;
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(schemaJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Create schema status: " + createResponse.statusCode());
        System.out.println(createResponse.body());
    }
}
