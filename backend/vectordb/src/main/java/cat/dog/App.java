package cat.dog;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cat.dog.repository.PostgresDbManager;
import cat.dog.repository.MemeVectorImporter;
import cat.dog.repository.PostgresSchemaCreator;
import cat.dog.repository.WeviateSchema;
import cat.dog.repository.ElasticSearchDBManager;
import cat.dog.repository.WeviateExtractedFaceImporter;
import cat.dog.utility.CSVLoader;
import cat.dog.repository.CelebVectorImporter;
import cat.dog.repository.ChromaCollectionSetup;
import cat.dog.repository.ChromaExtractedFaceImporter;

@SpringBootApplication
public class App implements CommandLineRunner
{
    public static void main( String[] args )
    {
        System.out.println("Starting");
        SpringApplication.run(App.class, args);
    }
    @Override
    public void run(String... args) throws Exception {

        startPythonServer();
        waitForPythonServer();

        setupPostgresSchema();
        addLabelTableToPostgres();
        addCelebTableToPostgres();
        setupWeaviateSchema();
        importMemeVectorsToWeaviate();
        setupElasticSearchIndices();
        importElasticSearchData();
        setupChromaCollection();
        importChromaExtractedFaces();
    }
    private void setupPostgresSchema() {
        PostgresSchemaCreator.createSchema("./../schema/schema_label.sql");
    }
    private void addLabelTableToPostgres() {
        // 1. Setup the connection (Same as you had before)
        PostgresDbManager dbManager = new PostgresDbManager();

        // 2. Check if DB is already set up
        if (dbManager.hasLabelData()) {
            System.out.println("Label table already has data. Skipping import.");
        } else {
            System.out.println("Label table is empty. Starting CSV import...");
            
            // 3. Run the Importer
            CSVLoader importer = new CSVLoader(dbManager);
            
            // Make sure this path is correct on your machine!
            importer.importLabelCSV("./../../DATA/archive/labels.csv"); 
        }
    }
    private void addCelebTableToPostgres() {
        // 1. Setup the connection (Same as you had before)
        PostgresDbManager dbManager = new PostgresDbManager();

        // 2. Check if DB is already set up
        if (dbManager.hasCelebData()) {
            System.out.println("Celeb table already has data. Skipping import.");
        } else {
            System.out.println("Celeb table is empty. Starting CSV import...");
            
            // 3. Run the Importer
            CSVLoader importer = new CSVLoader(dbManager);
            
            // Make sure this path is correct on your machine!
            importer.importCelebCSV("./../../DATA/celeb_mapping.csv"); 
        }
    }
    private void setupWeaviateSchema() throws Exception {
        WeviateSchema.createWeviateClass("MemeImage", "A meme image's precomputed CLIP vector");
        WeviateSchema.createWeviateClass("MemeImageCleaned", "A meme image without text and its precomputed CLIP vector");
        WeviateSchema.createCelebFacePickleSchema("CelebFaceEmbeddings", "An embedding of a celebrity's face");
        WeviateSchema.createExtractedFaceSchema("ExtractedFaceEmbeddings", "An embedding of an extracted face from a meme image");
    }
    private void importMemeVectorsToWeaviate() {
        MemeVectorImporter.importVectors("MemeImage", "./../../DATA/embeddings/");
        MemeVectorImporter.importVectors("MemeImageCleaned", "./../../DATA/embeddings_cleaned/");
        CelebVectorImporter.importCelebVectors();
        WeviateExtractedFaceImporter.importExtractedFaces();
    }
    private void setupElasticSearchIndices() {
        ElasticSearchDBManager dbManager = ElasticSearchDBManager.getInstance();
        dbManager.addCelebIndex();
        dbManager.addCaptionIndex();
    }
    private void importElasticSearchData() {
        ElasticSearchDBManager dbManager = ElasticSearchDBManager.getInstance();
        dbManager.importCelebNames();
        dbManager.importCaptions();
    }
    private void setupChromaCollection() throws Exception {
        ChromaCollectionSetup.createExtractedFaceCollection("face_vectors");
    }
    private void importChromaExtractedFaces() throws Exception {
        ChromaExtractedFaceImporter.importExtractedFaces();
    }
    private Process pythonProcess;
    /**
     * Launches python_server.py using the virtual environment's Python binary.
     * Runs in a background thread so it doesn't block Spring Boot.
     */
    private void startPythonServer() {
        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("🐍 Launching Python Server...");

                // 1. Get the absolute path of the project root (where pom.xml is)
                String projectRootPath = System.getProperty("user.dir");
                
                // 2. Define files using absolute paths to avoid confusion
                File pythonDir = new File(projectRootPath, "python");
                File pythonExec = new File(projectRootPath, ".venv/bin/python");
                
                // 3. Validation
                if (!pythonDir.exists()) {
                    System.err.println("❌ Error: Directory not found: " + pythonDir.getAbsolutePath());
                    return;
                }
                if (!pythonExec.exists()) {
                    System.err.println("❌ Error: Python interpreter not found: " + pythonExec.getAbsolutePath());
                    return;
                }

                // 4. Run Process
                // Command: /Users/.../vectordb/.venv/bin/python python_server.py
                // WorkDir: /Users/.../vectordb/python/ (So it can find ../models)
                ProcessBuilder pb = new ProcessBuilder(pythonExec.getAbsolutePath(), "python_server.py");
                pb.directory(pythonDir); 
                pb.inheritIO(); 
                
                pythonProcess = pb.start();

                // Shutdown Hook
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (pythonProcess != null && pythonProcess.isAlive()) {
                        System.out.println("🛑 Stopping Python Server...");
                        pythonProcess.destroy();
                    }
                }));

                int exitCode = pythonProcess.waitFor();
                if (exitCode != 0) System.err.println("❌ Python crashed with code: " + exitCode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true); 
        serverThread.start();
    }

    /**
     * Blocks execution until the Python server responds to a health check.
     */
    private void waitForPythonServer() throws InterruptedException {
        System.out.println("⏳ Waiting for Python models to load...");
        
        // Try for up to 15 minutes (first run may take time to download models)
        for (int i = 0; i < 450; i++) {
            if (isPythonServerUp()) {
                System.out.println("✅ Python Server is Ready!");
                return;
            }
            Thread.sleep(2000); // Wait 2s between checks
        }
        
        throw new RuntimeException("❌ Python server failed to start in time.");
    }

    /**
     * specific check to see if FastAPI is responding
     */
    private boolean isPythonServerUp() {
        try {
            URL url = new URL("http://127.0.0.1:8000/docs"); // Standard FastAPI docs endpoint
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000); 
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
