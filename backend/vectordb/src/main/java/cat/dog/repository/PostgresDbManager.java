package cat.dog.repository;

import cat.dog.dto.CelebRecord;
import cat.dog.dto.LabelRecord;
import cat.dog.model.Sentiment;
import cat.dog.utility.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PostgresDbManager {
    
    public PostgresDbManager() {
    }

    public void insertLabelRecord(LabelRecord record) {

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "INSERT INTO label (number, image_name, image_path, cleaned_image_path, text_ocr, text_corrected, overall_sentiment) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?::sentiment_level)";
        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmnt = conn.prepareStatement(sql)) {
            pstmnt.setInt(1, record.getNumber());
            pstmnt.setString(2, record.getImageName());
            pstmnt.setString(3, record.getImagePath());
            pstmnt.setString(4, record.getCleanedImagePath());
            pstmnt.setString(5, record.getTextOcr());
            pstmnt.setString(6, record.getTextCorrected());
            if (record.getSentiment() != null) {
                pstmnt.setString(7, record.getSentiment().toDbValue());
            } else {
                pstmnt.setNull(7, Types.VARCHAR);
            }
            pstmnt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void insertCelebRecord(CelebRecord celebRecord) {
        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "INSERT INTO celeb (image_path, celeb_name, classified_integer) " +
                     "VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmnt = conn.prepareStatement(sql)) {
            pstmnt.setString(1, celebRecord.getImagePath());
            pstmnt.setString(2, celebRecord.getCelebName());
            pstmnt.setInt(3, celebRecord.getClassifiedInteger());
            pstmnt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving celeb to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean hasLabelData() {

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "SELECT 1 FROM label LIMIT 1"; // Fast check for any row

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {

            return rs.next(); // Returns true if at least one row exists

        } catch (SQLException e) {
            // If the table doesn't exist, this throws an error. 
            // We assume false (no data) or handle accordingly.
            System.err.println("Check failed (Table might not exist): " + e.getMessage());
            return false;
        }
    }
    public boolean hasCelebData() {
        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "SELECT 1 FROM celeb LIMIT 1"; // Fast check for any row

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {

            return rs.next(); // Returns true if at least one row exists

        } catch (SQLException e) {
            // If the table doesn't exist, this throws an error. 
            // We assume false (no data) or handle accordingly.
            System.err.println("Check failed (Table might not exist): " + e.getMessage());
            return false;
        }
    }
    public LabelRecord getRecordByImageName(String imageName) {

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "SELECT number, image_name, image_path, cleaned_image_path, text_ocr, text_corrected, overall_sentiment FROM label WHERE image_name = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, imageName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Extract raw data from the database row
                    int number = rs.getInt("number");
                    String name = rs.getString("image_name");
                    String imagePath = rs.getString("image_path");
                    String cleanedImagePath = rs.getString("cleaned_image_path");
                    String ocr = rs.getString("text_ocr");
                    String corrected = rs.getString("text_corrected");
                    String sentimentStr = rs.getString("overall_sentiment");

                    // 2. Convert the String back to your Enum
                    // We handle null just in case the DB field is empty
                    Sentiment sentiment = null;
                    if (sentimentStr != null) {
                        sentiment = Sentiment.valueOf(sentimentStr); 
                        // Note: This works because we ensured DB strings match Enum names exactly
                    }

                    // 3. Return the populated DTO
                    return new LabelRecord(number, name, imagePath, cleanedImagePath, ocr, corrected, sentiment);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error searching DB: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Return null if no record was found
        return null; 
    }
    public List<LabelRecord> searchLabelByText(String userQuery) {
        List<LabelRecord> results = new ArrayList<>();

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return results;
        }

        // --- NEW SANITIZATION STEP ---
        // 1. Remove anything that is NOT a letter (a-z), number (0-9), or space.
        //    The ^ inside [] means "Not". So [^a-zA-Z0-9\s] means "Not alphanumeric or space".
        String cleanQuery = userQuery.replaceAll("[^a-zA-Z0-9\s]", "");
        
        // 2. NOW add the OR pipes to the clean text
        //    Input: "Old times! bigbangtheory"
        //    Clean: "Old times bigbangtheory"
        //    Final: "Old | times | bigbangtheory"
        String formattedQuery = cleanQuery.trim().replaceAll("\s+", " | ");

        // Check if the query became empty after cleaning (e.g. user entered "!!!")
        if (formattedQuery.isEmpty()) {
            return results;
        }

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        // 3. The SQL remains the same
        String sql = "SELECT * FROM label WHERE to_tsvector('english', text_corrected) @@ to_tsquery('english', ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, formattedQuery);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int number = rs.getInt("number");
                    String name = rs.getString("image_name");
                    String imagePath = rs.getString("image_path");
                    String cleanedImagePath = rs.getString("cleaned_image_path");
                    String ocr = rs.getString("text_ocr");
                    String corrected = rs.getString("text_corrected");
                    
                    String sentimentStr = rs.getString("overall_sentiment");
                    Sentiment sentiment = null;
                    if (sentimentStr != null) {
                        sentiment = Sentiment.valueOf(sentimentStr);
                    }

                    results.add(new LabelRecord(number, name, imagePath, cleanedImagePath, ocr, corrected, sentiment));
                }
            }

        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
    public List<CelebRecord> searchCelebByName(String celebName) {
        List<CelebRecord> results = new ArrayList<>();

        if (celebName == null || celebName.trim().isEmpty()) {
            return results;
        }

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "SELECT * FROM celeb WHERE celeb_name ILIKE ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + celebName + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String imagePath = rs.getString("image_path");
                    String name = rs.getString("celeb_name");
                    int classifiedInteger = rs.getInt("classified_integer");

                    results.add(new CelebRecord(imagePath, name, classifiedInteger));
                }
            }

        } catch (SQLException e) {
            System.err.println("Celeb search error: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    public List<String> getAllCelebNames() {
        List<String> celebNames = new ArrayList<>();

        String url = DatabaseConfig.getInstance().getJdbcUrl();
        String user = DatabaseConfig.getInstance().getPostgresUser();
        String password = DatabaseConfig.getInstance().getPostgresPassword();

        String sql = "SELECT DISTINCT celeb_name FROM celeb";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("celeb_name");
                celebNames.add(name);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving celeb names: " + e.getMessage());
            e.printStackTrace();
        }

        return celebNames;
    }
}
