package cat.dog.repository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import cat.dog.utility.DatabaseConfig;

public class PostgresSchemaCreator {

    public static void main(String[] args) {
        String filePath = "../schema/schema_label.sql";
        createSchema(filePath);
    }

    public static void createSchema(String filePath) {

        String URL = DatabaseConfig.getInstance().getJdbcUrl();
        String USER = DatabaseConfig.getInstance().getPostgresUser();
        String PASSWORD = DatabaseConfig.getInstance().getPostgresPassword();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Check if table 'label' exists
            ResultSet rsTable = stmt.executeQuery(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_name = 'label');"
            );
            rsTable.next();
            boolean tableExists = rsTable.getBoolean(1);

            // Check if enum 'sentiment_level' exists
            ResultSet rsEnum = stmt.executeQuery(
                    "SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'sentiment_level');"
            );
            rsEnum.next();
            boolean enumExists = rsEnum.getBoolean(1);

            // Check if celeb table exists
            ResultSet rsCelebTable = stmt.executeQuery(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_name = 'celeb');"
            );
            rsCelebTable.next();
            boolean celebTableExists = rsCelebTable.getBoolean(1);

            if (tableExists && enumExists && celebTableExists) {
                System.out.println("Schema already exists. Skipping creation.");
                return;
            }

            // Read SQL file and execute
            String sql = new String(Files.readAllBytes(Paths.get(filePath)));
            stmt.execute(sql);
            System.out.println("Schema created successfully.");

        } catch (SQLException e) {
            System.err.println("Database error:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error reading SQL file:");
            e.printStackTrace();
        }
    }
}
