package cat.dog.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class LLMQueryProcessor {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private static final String SERVER_URL = "http://127.0.0.1:8000/analyze";

    public LLMQueryProcessor() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Map<String, Object> processQuery(String queryText) {
        try {
            // 1. Create JSON Payload: {"query": "..."}
            Map<String, String> payload = new HashMap<>();
            payload.put("query", queryText);
            String requestBody = objectMapper.writeValueAsString(payload);

            // 2. Build HTTP Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 3. Send Request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Server returned error: " + response.body());
            }

            // 4. Parse Response
            // The Python server now returns clean JSON, so we just map it.
            return objectMapper.readValue(response.body(), Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to communicate with AI Server: " + e.getMessage());
            return errorMap;
        }
    }

    // ==========================================
    // MAIN METHOD FOR TESTING
    // ==========================================
    public static void main(String[] args) {
        LLMQueryProcessor processor = new LLMQueryProcessor();
        
        // Ensure you run 'python meme_server.py' in a separate terminal first!
        String testQuery = "PERFECTLY HEALTHY GIVES BILLIONS TO CURE DISEASE KEEPS BILLIONS DIES OF CANCER";

        System.out.println("--------------------------------------------------");
        System.out.println("Sending Request to Python Server...");

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = processor.processQuery(testQuery);
        long endTime = System.currentTimeMillis();

        if (result.containsKey("error")) {
            System.err.println("ERROR: " + result.get("error"));
            System.err.println("Did you start the Python server?");
        } else {
            System.out.println("Success! (" + (endTime - startTime) + "ms)");
            result.forEach((key, value) -> System.out.println("   - " + key + ": " + value));
        }
    }
}