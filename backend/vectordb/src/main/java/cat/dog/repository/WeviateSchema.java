package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import cat.dog.utility.DatabaseConfig;

public class WeviateSchema {

    public static void main(String[] args) throws Exception {
        createWeviateClass("MemeImage", "A meme image's precomputed CLIP vector");
        createWeviateClass("MemeImageCleaned", "A meme image without text and its precomputed CLIP vector");
    }

    public static void createWeviateClass(String className, String description) throws Exception {
        final String BASE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/schema/";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest getSchemaRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getSchemaRequest, HttpResponse.BodyHandlers.ofString());

        boolean classExists = getResponse.body().contains("\"class\":\"" + className + "\"");
        if (classExists) {
            System.out.println("Class '" + className + "' already exists. Skipping creation.");
            return;
        }

        String schemaJson = """
        {
          "class": "%s",
          "description": "%s",
          "vectorizer": "none",
          "properties": [
            {"name": "name", "dataType": ["string"]}
          ]
        }
        """.formatted(className, description);

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(schemaJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Create class '" + className + "' status: " + createResponse.statusCode());
        System.out.println(createResponse.body());
    }
}
