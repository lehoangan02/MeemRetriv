package cat.dog.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LLMQueryProcessor {

    private final ObjectMapper objectMapper;

    public LLMQueryProcessor() {
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> processQuery(String queryText) {
        try {
            // 1. Define paths relative to project root
            // Ensure this points to where your python folder actually is
            String pythonEnvPath = ".venv/bin/python"; 
            String scriptPath = "python/MemeLLMProcessor.py";

            // Verify file existence
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                throw new RuntimeException("Python script not found at: " + scriptFile.getAbsolutePath() +
                        ". Check your path.");
            }

            // 2. Build the process
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonEnvPath,
                    scriptPath,
                    queryText
            );

            processBuilder.directory(new File("."));
            Process process = processBuilder.start();

            // 3. Read the Output
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            // 4. Wait for finish
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python script exited with error code: " + exitCode);
            }

            // 5. Clean and Parse JSON
            String jsonString = extractJsonString(output);

            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Failed to process query: " + e.getMessage());
            return errorMap;
        }
    }

    /**
     * Helper to extract valid JSON. 
     * Improved to skip Python dictionaries (single quotes) and find actual JSON (double quotes).
     */
    private String extractJsonString(String rawOutput) {
        // Priority 1: Look for markdown code block ```json ... ```
        Pattern markdownPattern = Pattern.compile("```json(.*?)```", Pattern.DOTALL);
        Matcher markdownMatcher = markdownPattern.matcher(rawOutput);
        
        if (markdownMatcher.find()) {
            return markdownMatcher.group(1).trim();
        }

        // Priority 2: Look for the first "{" followed by a double quote
        // This regex matches '{' followed by optional whitespace and '"'
        // It specifically avoids matching "{'" which is common in Python dict prints
        Pattern jsonPattern = Pattern.compile("\\{\\s*\"");
        Matcher jsonMatcher = jsonPattern.matcher(rawOutput);

        if (jsonMatcher.find()) {
            int startIndex = jsonMatcher.start();
            // Find the last closing brace to capture the full object
            int endIndex = rawOutput.lastIndexOf("}");
            
            if (endIndex > startIndex) {
                return rawOutput.substring(startIndex, endIndex + 1);
            }
        }

        // Fallback: return original (will likely fail if dirty)
        return rawOutput;
    }

    // ==========================================
    // MAIN METHOD FOR TESTING
    // ==========================================
    public static void main(String[] args) {
        LLMQueryProcessor processor = new LLMQueryProcessor();
        String testQuery = "Obama giving Obama a medal.";

        System.out.println("--------------------------------------------------");
        System.out.println("Starting Test...");
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = processor.processQuery(testQuery);
        long endTime = System.currentTimeMillis();

        if (result.containsKey("error")) {
            System.err.println("❌ ERROR: " + result.get("error"));
        } else {
            System.out.println("✅ Success! (" + (endTime - startTime) + "ms)");
            System.out.println("Parsed Output:");
            result.forEach((key, value) -> System.out.println("   • " + key + ": " + value));
        }
    }
}