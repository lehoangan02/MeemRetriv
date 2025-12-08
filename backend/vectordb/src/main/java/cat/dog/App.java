package cat.dog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import cat.dog.repository.LabelDbManager;
import cat.dog.service.LabelCSVLoader;
import cat.dog.service.PostgresSchemaCreator;

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
        setupPostgresSchema();
        addLabelTableToPostgres();
    }
    private void setupPostgresSchema() {
        PostgresSchemaCreator.createSchema("../schema/schema_label.sql");
    }
    private void addLabelTableToPostgres() {
        // 1. Setup the connection (Same as you had before)
        LabelDbManager dbManager = new LabelDbManager();

        // 2. Check if DB is already set up
        if (dbManager.hasData()) {
            System.out.println("Database already has data. Skipping import.");
        } else {
            System.out.println("Database is empty. Starting CSV import...");
            
            // 3. Run the Importer
            LabelCSVLoader importer = new LabelCSVLoader(dbManager);
            
            // Make sure this path is correct on your machine!
            importer.importCSV("./../../DATA/archive/labels.csv"); 
        }
    }
}
