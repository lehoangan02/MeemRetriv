package cat.dog.utility;

import cat.dog.dto.LabelRecord;
import cat.dog.dto.CelebRecord;
import cat.dog.model.Sentiment;
import cat.dog.repository.PostgresDbManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;

public class CSVLoader {
    private final PostgresDbManager dbManager;
    
    public CSVLoader(PostgresDbManager dbManager) {
        this.dbManager = dbManager;
    }

    public void importLabelCSV(String filePath) {

        // example filepath: "./../../DATA/archive/labels.csv"
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
                    // strip the file extension from image name, e.g., image_1.jpg -> image_1

                    String extension = "";
                    int dotIndex = imageName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        extension = imageName.substring(dotIndex);
                        imageName = imageName.substring(0, dotIndex);
                    }

                    String imagePath = "./../../DATA/archive/images/images/" + imageName + extension;
                    // example: image_1.jpg -> ./../../DATA/archive/images/images/image_1.jpg

                    String cleanedImagePath = "./../../DATA/cleaned/" + imageName + extension;
                    // example: image_1.jpg -> ./../../DATA/cleaned/image_1.jpg

                    String textOcr = record.get(2);
                    String textCorrected = record.get(3);
                    
                    String sentimentStr = record.get(4);

                    Sentiment sentiment = Sentiment.valueOf(sentimentStr.toLowerCase().trim());

                    LabelRecord labelRecord = new LabelRecord(number, imageName, imagePath, cleanedImagePath, textOcr, textCorrected, sentiment);

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
    public void importCelebCSV(String filePath) {

        // example filepath: "./../../DATA/celeb_mapping.csv"
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
                    String imagePath = record.get(0);

                    String celebName = record.get(1);

                    int classifiedInteger = Integer.parseInt(record.get(2));

                    CelebRecord celebRecord = new CelebRecord(imagePath, celebName, classifiedInteger);

                    dbManager.insertCelebRecord(celebRecord);

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