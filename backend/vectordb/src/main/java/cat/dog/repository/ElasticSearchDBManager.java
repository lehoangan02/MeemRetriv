package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import cat.dog.utility.DatabaseConfig;

import java.net.http.HttpRequest;
import java.util.List;

public class ElasticSearchDBManager {
    // private static ElasticSearchDBManager INSTANCE = new ElasticSearchDBManager();
    // private ElasticSearchDBManager() {
    // }
    
    private boolean checkIfIndexExists(String indexName) {
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + indexName))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            // Elasticsearch returns:
            // 200 = exists
            // 404 = does not exist
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean hasAnyDocuments(String indexName) {
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + indexName + "/_count"))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // If index does not exist or error
            if (response.statusCode() != 200) {
                return false;
            }

            // Response looks like:
            // { "count": 5, "_shards": {...} }
            String responseBody = response.body();

            // Very simple parse (no JSON library needed)
            // Extract the number after `"count":`
            int countIndex = responseBody.indexOf("\"count\":");
            if (countIndex == -1) return false;

            String afterCount = responseBody.substring(countIndex + 8).trim();
            String numberOnly = afterCount.split(",")[0].replaceAll("[^0-9]", "");

            int count = Integer.parseInt(numberOnly);

            return count > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void addCelebIndex() {
        // check if celebrities already exists
        String indexName = "celebrities";
        boolean exists = checkIfIndexExists(indexName);
        if (exists) {
            System.out.println("Index " + indexName + " already exists. Skipping creation.");
            return;
        }

        // check if there is any document in the index
        boolean hasDocs = hasAnyDocuments(indexName);
        if (hasDocs) {
            System.out.println("Index " + indexName + " already has documents. Skipping creation.");
            return;
        }

        // create the index
        createCelebIndex();
    }
    private void createCelebIndex() {
        String indexName = "celebrities";
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();

        String jsonBody = """
        {
        "mappings": {
            "properties": {
            "name": {
                "type": "text"
            }
            }
        }
        }
        """;

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + indexName))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Create index response: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importCelebNames() {
        PostgresDbManager pgManager = new PostgresDbManager();
        List<String> celebNames = pgManager.getAllCelebNames();
        String indexName = "celebrities";
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();

        HttpClient client = HttpClient.newHttpClient();

        for (String name : celebNames) {
            try {
                // Document JSON
                String jsonBody = """
                {
                "name": "%s"
                }
                """.formatted(name.replace("\"", "\\\"")); // escape quotes

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/" + indexName + "/_doc"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Inserted: " + name + " → " + response.statusCode());

            } catch (Exception e) {
                System.out.println("Failed to insert: " + name);
                e.printStackTrace();
            }
        }

        System.out.println("Finished importing " + celebNames.size() + " celebrity names.");
    }

    public void main(String[] args) {
        String indexName = "celebrities";
        // boolean exists = checkIfIndexExists(indexName);
        // if (exists) {
        //     System.out.println("Index " + indexName + " exists.");
        // } else {
        //     System.out.println("Index " + indexName + " does not exist.");
        // }
        // boolean hasDocs = hasAnyDocuments(indexName);
        // if (hasDocs) {
        //     System.out.println("Index " + indexName + " has documents.");
        // } else {
        //     System.out.println("Index " + indexName + " has no documents.");
        // }
        // addCelebIndex();
        importCelebNames();
    }
}
