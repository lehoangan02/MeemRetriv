package cat.dog.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RemoveMemeText {

    private static final String PYTHON_PATH =
            "./.venv/bin/python";   // relative path to python in your venv
    private static final String SCRIPT_PATH =
            "./python/remove_meme_text.py"; // relative path to script

    public static void clean(String inputPath, String outputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH,
                    SCRIPT_PATH,
                    "--input", inputPath,
                    "--output", outputPath
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[PYTHON] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python failed with exit code " + exitCode);
            }

            System.out.println("Text removed successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to run python script.", e);
        }
    }

    public static void main(String[] args) {
        clean("./received_images/query_image.png", "./received_images/cleaned_query_image.png");
    }
}
