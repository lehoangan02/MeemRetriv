package cat.dog.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import cat.dog.utility.DatabaseConfig;

public class DropTablePostgres {
    public static void main(String[] args) {
        // check if there is an argument
        if (args.length == 0) {
            System.err.println("Error: No table name provided.");
            System.out.println("Usage: java cat.dog.service.DropTablePostgres <TableName>");
            return;
        }
        String tableName = args[0];
        dropTable(tableName);
    }
    public static void dropTable(String tableName) {
        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String sql = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
            stmt.executeUpdate(sql);

            System.out.println("Table " + tableName + " deleted successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void dropEnum(String enumName) {
        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String sql = "DO $$ BEGIN " +
                         "IF EXISTS (SELECT 1 FROM pg_type WHERE typname = '" + enumName + "') THEN " +
                         "EXECUTE 'DROP TYPE " + enumName + "'; " +
                         "END IF; " +
                         "END $$;";
            stmt.execute(sql);

            System.out.println("Enum " + enumName + " deleted successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
