package cat.dog.utility;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseConfig {
    private static final DatabaseConfig INSTANCE = new DatabaseConfig();

    // Class fields to hold postgres configuration
    private final String server;
    private final int port;
    private final String dbName;
    private final String user;
    private final String password;

    private DatabaseConfig() {
        Dotenv dotenv = Dotenv.configure()
            .directory("./")  // Look in the parent directory
            .load();
        
        System.out.println("Loading database configuration from .env file...");

        this.server = dotenv.get("POSTGRES_SERVER", "localhost");
        this.port = Integer.parseInt(dotenv.get("POSTGRES_PORT", "5432"));
        this.dbName = dotenv.get("POSTGRES_DB", "label_db");
        this.user = dotenv.get("POSTGRES_USER", "postgres");
        this.password = dotenv.get("POSTGRES_PASSWORD", "123456789");

        if (server == null || dbName == null || user == null || password == null) {
            throw new RuntimeException("Missing required environment variables in .env file!");
        }
    }

    public static DatabaseConfig getInstance() {
        return INSTANCE;
    }

    public String getPostgresServer() { return server; }
    public int getPostgresPort() { return port; }
    public String getPostgresDBName() { return dbName; }
    public String getPostgresUser() { return user; }
    public String getPostgresPassword() { return password; }
    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", server, port, dbName);
    }

    // Class fields to hold weviate configuration
    private final String wevivate_url = "http://127.0.0.1:8080/v1/";


    public String getWeviateUrl() {
        return wevivate_url;
    }

    // Class fields to hold elasticsearch configuration
    private final String elasticsearch_url = "http://localhost:9200";
    public String getElasticsearchUrl() {
        return elasticsearch_url;
    }
}