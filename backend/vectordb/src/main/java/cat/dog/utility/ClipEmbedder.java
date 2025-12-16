package cat.dog.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.stream.Collectors;

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

        JsonObject obj = new JsonObject();
        obj.addProperty("text", textQuery);
        String jsonPayload = obj.toString();

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
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream stream = (code == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String resp = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).lines().collect(Collectors.joining());

            if (code != 200) {
                System.err.println("ClipEmbedder error [" + code + "]: " + resp);
                return null;
            }

            JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
            return json.getAsJsonArray("embedding").toString();

        } catch (Exception e) {
            System.err.println("ClipEmbedder failure: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
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