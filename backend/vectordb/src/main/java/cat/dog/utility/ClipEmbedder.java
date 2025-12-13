package cat.dog.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for generating MobileCLIP embeddings for images and text.
 * * UPDATED: Now acts as a client for the MemeLLM FastAPI server running on localhost:8000.
 * Ensure the Python server is running before using this utility.
 */
public class ClipEmbedder {

    private static final String API_BASE_URL = "http://127.0.0.1:8000";
    private static final String ENDPOINT_TEXT = API_BASE_URL + "/embed_text";
    private static final String ENDPOINT_IMAGE = API_BASE_URL + "/embed_image";

    public static void main(String[] args) {

        // 1. Text Embedding Example
        String textQuery = "A funny minion meme";
        String textNpy = "text_query_vector.json"; // Changed extension to .json as we save text representation now
        System.out.println("Embedding text: \"" + textQuery + "\" -> " + textNpy);
        
        String resText = ClipEmbedder.embedText(textQuery, textNpy);
        System.out.println("Result Preview: " + (resText != null ? resText.substring(0, 50) + "..." : "null"));

        // 2. Image Embedding Example
        String imagePath = "./image_3889.jpg"; // Ensure this file exists for test
        String imageNpy = "image_3889_vector.json";
        System.out.println("Embedding image: " + imagePath + " -> " + imageNpy);
        
        String resImg = ClipEmbedder.embedImage(imagePath, imageNpy);
        System.out.println("Result Preview: " + (resImg != null ? resImg.substring(0, 50) + "..." : "null"));
        
        System.out.println("Done.");
    }

    /**
     * Overload for text embedding without saving to file.
     */
    public static String embedText(String textQuery) {
        return embedText(textQuery, null);
    }

    /**
     * Generates a normalized vector embedding for the given text.
     * @param textQuery The text to embed.
     * @param saveNpyPath Optional path to save the vector. Note: This now saves as JSON text, not binary .npy.
     * @return JSON string representation of the vector.
     */
    public static String embedText(String textQuery, String saveNpyPath) {
        if (textQuery == null || textQuery.isEmpty()) return null;

        // Escape text for JSON
        String safeText = textQuery.replace("\\", "\\\\").replace("\"", "\\\"");
        String jsonPayload = "{\"text\": \"" + safeText + "\"}";

        String vectorJson = sendRequest(ENDPOINT_TEXT, jsonPayload);

        if (vectorJson != null && saveNpyPath != null) {
            saveVectorToFile(vectorJson, saveNpyPath);
        }

        return vectorJson;
    }

    /**
     * Overload for image embedding without saving to file.
     */
    public static String embedImage(String imagePath) {
        return embedImage(imagePath, null);
    }

    /**
     * Processes a list of images.
     * Note: The current FastAPI server does not support native batching, so this 
     * iterates and calls the singular endpoint for each image.
     */
    public static List<String> embedImageBatch(List<String> imagePaths) {
        List<String> results = new ArrayList<>();
        
        for (String path : imagePaths) {
            try {
                // Call singular method
                String result = embedImage(path, null);
                results.add(result);
            } catch (Exception e) {
                System.err.println("Error processing batch item " + path + ": " + e.getMessage());
                results.add(null);
            }
        }
        
        return results;
    }

    /**
     * Generates a normalized vector embedding for the image at the given path.
     * @param imagePath Absolute or relative path to the image file.
     * @param saveNpyPath Optional path to save the vector. Note: This now saves as JSON text, not binary .npy.
     * @return JSON string representation of the vector.
     */
    public static String embedImage(String imagePath, String saveNpyPath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.err.println("ClipEmbedder Error: Image file does not exist at " + imagePath);
            return null;
        }

        // Resolve absolute path to send to server
        String absPath = imageFile.getAbsolutePath().replace("\\", "\\\\");
        String jsonPayload = "{\"image_path\": \"" + absPath + "\"}";

        String vectorJson = sendRequest(ENDPOINT_IMAGE, jsonPayload);

        if (vectorJson != null && saveNpyPath != null) {
            saveVectorToFile(vectorJson, saveNpyPath);
        }

        return vectorJson;
    }

    /**
     * Helper to send HTTP POST request to the FastAPI server.
     */
    private static String sendRequest(String endpoint, String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Write Request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check Status
            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("ClipEmbedder Server Error [" + code + "] on " + endpoint);
                return null;
            }

            // Read Response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Parse JSON response manually to extract just the embedding array
            // Expected format: {"embedding": [0.1, 0.2, ...]}
            String respStr = response.toString();
            int startIndex = respStr.indexOf("[");
            int endIndex = respStr.lastIndexOf("]");
            
            if (startIndex != -1 && endIndex != -1) {
                // Return just the list part to match previous interface behavior
                return respStr.substring(startIndex, endIndex + 1);
            }

            return null;

        } catch (Exception e) {
            System.err.println("ClipEmbedder Connection Failure: " + e.getMessage());
            System.err.println("Ensure the FastAPI server is running on " + API_BASE_URL);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Saves the vector JSON string to a file.
     * Replaces the old numpy binary save functionality.
     */
    private static void saveVectorToFile(String vectorData, String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(vectorData);
        } catch (Exception e) {
            System.err.println("ClipEmbedder Error saving file: " + e.getMessage());
        }
    }
}