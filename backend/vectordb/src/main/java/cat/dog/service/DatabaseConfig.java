package cat.dog.service;

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
            .directory("../")  // Look in the parent directory
            .ignoreIfMissing()
            .load();

        this.server = dotenv.get("POSTGRES_SERVER", "localhost");
        this.port = Integer.parseInt(dotenv.get("POSTGRES_PORT", "5432"));
        this.dbName = dotenv.get("POSTGRES_DB", "label_db");
        this.user = dotenv.get("POSTGRES_USER", "postgres");
        this.password = dotenv.get("POSTGRES_PASSWORD", "123456789");
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
}