package cat.dog.service;

import cat.dog.dto.LabelRecord;
import cat.dog.model.Sentiment;
import cat.dog.repository.LabelDbManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;

public class LabelCSVLoader {
    private final LabelDbManager dbManager;
    
    public LabelCSVLoader(LabelDbManager dbManager) {
        this.dbManager = dbManager;
    }

    public void importCSV(String filePath) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setAllowMissingColumnNames(true)
                .build();
        int count = 0;
        try (Reader in = new FileReader(filePath)) {
            Iterable<CSVRecord> records = format.parse(in);
            for (CSVRecord record : records) {
                try {
                    int number = Integer.parseInt(record.get(0));
                    String imageName = record.get(1);
                    String textOcr = record.get(2);
                    String textCorrected = record.get(3);
                    
                    String sentimentStr = record.get(4);

                    Sentiment sentiment = Sentiment.valueOf(sentimentStr.toLowerCase().trim());

                    LabelRecord labelRecord = new LabelRecord(number, imageName, textOcr, textCorrected, sentiment);

                    dbManager.insertLabelRecord(labelRecord);

                    count++;

                } catch (Exception e) {
                    System.err.println("Error processing row " + count + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Import Finished! Processed " + count + " records.");
    }
}