package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import cat.dog.utility.DatabaseConfig;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.AbstractMap;

public class ElasticSearchDBManager {
    private static ElasticSearchDBManager INSTANCE = new ElasticSearchDBManager();
    private ElasticSearchDBManager() {

    }
    public static ElasticSearchDBManager getInstance() {
        return INSTANCE;
    }
    
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

    public List<String> fuzzySearchCelebNames(String query, float minScore) {
        List<String> results = new java.util.ArrayList<>();
        String indexName = "celebrities";
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();

        try {
            HttpClient client = HttpClient.newHttpClient();

            String jsonBody = """
            {
            "query": {
                "match": {
                "name": {
                    "query": "%s",
                    "fuzziness": "AUTO"
                }
                }
            }
            }
            """.formatted(query.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + indexName + "/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Search failed: " + response.body());
                return results;
            }

            String body = response.body();

            // Parse hits array manually (simple string parse)
            String[] parts = body.split("\"hits\":\\s*\\[");
            if (parts.length < 2) return results;

            String hitsSection = parts[1].split("]")[0];

            String[] hitEntries = hitsSection.split("\\},\\s*\\{");

            for (String entry : hitEntries) {
                // Extract score
                int scoreIndex = entry.indexOf("\"_score\"");
                if (scoreIndex == -1) continue;

                String afterScore = entry.substring(scoreIndex + 8).trim();
                String scoreStr = afterScore.split(",")[0].replaceAll("[^0-9\\.]", "");
                float score = Float.parseFloat(scoreStr);

                if (score < minScore) continue;

                // Extract name
                int nameIndex = entry.indexOf("\"name\"");
                if (nameIndex == -1) continue;

                String afterName = entry.substring(nameIndex + 6).trim();
                String nameValue = afterName.split(":")[1]
                    .trim()
                    .replaceAll("[\"}]", "")
                    .trim();

                results.add(nameValue);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public void addCaptionIndex() {
        String indexName = "captions";
        
        // Optional: Check if it exists first to avoid errors
        if (checkIfIndexExists(indexName)) {
            System.out.println("Index " + indexName + " already exists. Skipping creation.");
            return;
        }

        String url = DatabaseConfig.getInstance().getElasticsearchUrl();

        String jsonBody = """
        {
          "mappings": {
            "properties": {
              "caption": { "type": "text" },
              "ref_id": { "type": "integer" }
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

            System.out.println("Create " + indexName + " index response: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importCaptions() {
        PostgresDbManager pgManager = new PostgresDbManager();
        // Fetch the list of pairs (ID, Caption)
        List<Map.Entry<Integer, String>> captions = pgManager.getAllCaptions();
        
        String indexName = "captions";
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();

        HttpClient client = HttpClient.newHttpClient();
        
        // Iterate over the entries
        for (Map.Entry<Integer, String> entry : captions) {
            int refId = entry.getKey();      // The actual DB ID
            String caption = entry.getValue(); // The Caption text

            try {
                if (caption == null) continue; // Skip null captions

                // Escape quotes and remove newlines to prevent broken JSON
                String safeCaption = caption
                    .replace("\"", "\\\"")   // Escape double quotes
                    .replace("\n", " ")      // Replace newlines with space
                    .replace("\r", "");      // Remove carriage returns

                // Use refId instead of idCounter
                String jsonBody = """
                {
                "caption": "%s",
                "ref_id": %d
                }
                """.formatted(safeCaption, refId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/" + indexName + "/_doc")) // You can also use /_doc/<refId> to force the ID
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Inserted caption ID " + refId + " → Status: " + response.statusCode());

            } catch (Exception e) {
                System.err.println("Failed to insert caption ID: " + refId);
                e.printStackTrace();
            }
        }
    }

    public List<Map.Entry<Integer, String>> fuzzySearchCaptions(String query, float minScore) {
        String indexName = "captions";
        String url = DatabaseConfig.getInstance().getElasticsearchUrl();
        
        // Key is Integer (ID), Value is String (Caption)
        List<Map.Entry<Integer, String>> foundCaptions = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            String jsonBody = """
            {
            "query": {
                "match": {
                "caption": {
                    "query": "%s",
                    "fuzziness": "AUTO"
                }
                }
            }
            }
            """.formatted(query.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/" + indexName + "/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Search failed: " + response.body());
                return foundCaptions;
            }

            String body = response.body();

            JsonNode rootNode = mapper.readTree(body);
            JsonNode hitsNode = rootNode.path("hits").path("hits");

            if (hitsNode.isArray()) {
                for (JsonNode hit : hitsNode) {
                    double score = hit.path("_score").asDouble();
                    
                    if (score >= minScore) {
                        JsonNode source = hit.path("_source");
                        
                        String caption = source.path("caption").asText();
                        int refId = source.path("ref_id").asInt();

                        // Key = refId, Value = caption
                        foundCaptions.add(new AbstractMap.SimpleEntry<>(refId, caption));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return foundCaptions;
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
        // importCelebNames();
        // List<String> results = fuzzySearchCelebNames("Obama", 6.0f);
        // System.out.println("Fuzzy search results for 'Obama': " + results);
        // addCaptionIndex();
        importCaptions();
        // List<Map.Entry<Integer, String>> captionResults = fuzzySearchCaptions("funny cat", 5.0f);
        // for (Map.Entry<Integer, String> entry : captionResults) {
        //     System.out.println("Ref ID: " + entry.getKey() + " | Caption: " + entry.getValue());
        // }
    }
}
