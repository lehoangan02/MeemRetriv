package cat.dog.testing;

import java.util.List;

import cat.dog.model.LabelDbManager;
import cat.dog.dto.LabelRecord;

public class SearchImageByTextCorrected {
    public static void main(String[] args) {
        LabelDbManager dbManager = new LabelDbManager(
            "jdbc:postgresql://localhost:5432/label_db", 
            "postgres", 
            "123456789" // <--- Replace with your actual password
        );
        List<LabelRecord> records = dbManager.searchByText("Old times! bigbangtheory");
        if (!records.isEmpty()) {
            System.out.println("Records found:\n");
            System.out.println("Total records found: " + records.size());
            for (LabelRecord record : records) {
                System.out.println("Number: " + record.getNumber());
                System.out.println("Image Name: " + record.getImageName());
                System.out.println("Text OCR: " + record.getTextOcr());
                System.out.println("Text Corrected: " + record.getTextCorrected());
                System.out.println("Sentiment: " + record.getSentiment());
            }
        } else {
            System.out.println("No record found for the given image name.");
        }
    }
}
