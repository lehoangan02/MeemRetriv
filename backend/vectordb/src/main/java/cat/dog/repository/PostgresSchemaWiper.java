package cat.dog.repository;

import cat.dog.repository.DropTablePostgres;

public class PostgresSchemaWiper {

    public static void main(String[] args) {
        // Delete tables
        DropTablePostgres.dropTable("celeb");
        DropTablePostgres.dropTable("label");

        // Delete enum
        DropTablePostgres.dropEnum("sentiment_level");

        System.out.println("Schema wiped successfully.");
    }
}
