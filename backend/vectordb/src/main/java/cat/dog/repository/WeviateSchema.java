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
        createCelebFacePickleSchema("CelebFaceEmbeddings", "An embedding of a celebrity's face");
        createExtractedFaceSchema("ExtractedFaceEmbeddings", "An embedding of an extracted face from a meme image");
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
    public static void createCelebFacePickleSchema(String className, String description) throws Exception {
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
            {
              "name": "name", 
              "dataType": ["string"],
              "description": "The name of the celebrity"
            },
            {
              "name": "filePath", 
              "dataType": ["string"],
              "description": "The relative path to the image file"
            }
          ]
        }
        """.formatted(className, description);

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(schemaJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Create Celeb class '" + className + "' status: " + createResponse.statusCode());
        System.out.println(createResponse.body());
    }
    public static void createExtractedFaceSchema(String className, String description) throws Exception {
        final String BASE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/schema/";
        HttpClient client = HttpClient.newHttpClient();

        // 1. Check if schema already exists
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

        // 2. Define Schema
        // We do not add a "vector" property here. In Weaviate, the vector is a 
        // first-class citizen attached to the object, not a property in the "properties" list.
        String schemaJson = """
        {
          "class": "%s",
          "description": "%s",
          "vectorizer": "none",
          "properties": [
            {
              "name": "imageName", 
              "dataType": ["string"],
              "description": "The parent image identifier (e.g., image_1)"
            },
            {
              "name": "filePath", 
              "dataType": ["string"],
              "description": "The relative path to the specific face file (e.g., ./extracted_faces/image_1/face_1.jpg)"
            }
          ]
        }
        """.formatted(className, description);

        // 3. Post Schema
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(schemaJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Create ExtractedFace class '" + className + "' status: " + createResponse.statusCode());
        System.out.println(createResponse.body());
    }
}
