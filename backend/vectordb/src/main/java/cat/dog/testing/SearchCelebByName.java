package cat.dog.testing;

import cat.dog.dto.CelebRecord;
import cat.dog.repository.PostgresDbManager;

import java.util.List;

public class SearchCelebByName {
    public static void main(String[] args) {
        PostgresDbManager dbManager = new PostgresDbManager();
        List<CelebRecord> records = dbManager.searchCelebByName("Cristiano Ronaldo");
        if (!records.isEmpty()) {
            System.out.println("Records found:\n");
            System.out.println("Total records found: " + records.size());
            for (CelebRecord record : records) {
                System.out.println("Image Path: " + record.getImagePath());
                System.out.println("Celeb Name: " + record.getCelebName());
                System.out.println("Classified Integer: " + record.getClassifiedInteger());
            }
        } else {
            System.out.println("No record found for the given celeb name.");
        }
    }
}
