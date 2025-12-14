package cat.dog.repository;

import cat.dog.utility.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfigPostgresDatabase {

    /**
     * Check if a database exists.
     * @param dbName the database name
     * @return true if exists, false otherwise
     */
    private static boolean databaseExists(String dbName) {
        String url = "jdbc:postgresql://" + DatabaseConfig.getInstance().getPostgresServer() + ":" +
        DatabaseConfig.getInstance().getPostgresPort() + "/postgres";

        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next();

        } catch (SQLException e) {
            System.err.println("Error checking if database exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create a PostgreSQL database if it doesn't already exist.
     * @param dbName the name of the database to create
     */
    public static void createDatabase(String dbName) {
        if (databaseExists(dbName)) {
            System.out.println("Database \"" + dbName + "\" already exists.");
            return;
        }

        System.out.println("Creating database \"" + dbName + "\"...");

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "CREATE DATABASE \"" + dbName + "\"";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("Database \"" + dbName + "\" created successfully.");

        } catch (SQLException e) {
            System.err.println("Error creating database \"" + dbName + "\": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Drop a PostgreSQL database if it exists.
     * @param dbName the name of the database to drop
     */
    public static void dropDatabase(String dbName) {
        if (!databaseExists(dbName)) {
            System.out.println("Database \"" + dbName + "\" does not exist.");
            return;
        }

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "DROP DATABASE \"" + dbName + "\"";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
            System.out.println("Database \"" + dbName + "\" dropped successfully.");

        } catch (SQLException e) {
            System.err.println("Error dropping database \"" + dbName + "\": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // check if database exists
        String testDbName = "label_db";
        boolean exists = ConfigPostgresDatabase.databaseExists(testDbName);
        System.out.println("Database \"" + testDbName + "\" exists: " + exists);
    }
}
