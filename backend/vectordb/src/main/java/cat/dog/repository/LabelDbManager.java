package cat.dog.repository;

import cat.dog.dto.LabelRecord;
import cat.dog.model.Sentiment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class LabelDbManager {
    private String url;
    private String user;
    private String password;
    
    public LabelDbManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void insertLabelRecord(LabelRecord record) {
        String sql = "INSERT INTO label (number, image_name, text_ocr, text_corrected, overall_sentiment) " +
                     "VALUES (?, ?, ?, ?, ?::sentiment_level)";
        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmnt = conn.prepareStatement(sql)) {
            pstmnt.setInt(1, record.getNumber());
            pstmnt.setString(2, record.getImageName());
            pstmnt.setString(3, record.getTextOcr());
            pstmnt.setString(4, record.getTextCorrected());
            if (record.getSentiment() != null) {
                pstmnt.setString(5, record.getSentiment().toDbValue());
            } else {
                pstmnt.setNull(5, Types.VARCHAR);
            }
            pstmnt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean hasData() {
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
    public LabelRecord getRecordByImageName(String imageName) {
        String sql = "SELECT number, image_name, text_ocr, text_corrected, overall_sentiment FROM label WHERE image_name = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, imageName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Extract raw data from the database row
                    int number = rs.getInt("number");
                    String name = rs.getString("image_name");
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
                    return new LabelRecord(number, name, ocr, corrected, sentiment);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error searching DB: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Return null if no record was found
        return null; 
    }
    public List<LabelRecord> searchByText(String userQuery) {
        List<LabelRecord> results = new ArrayList<>();

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return results;
        }

        // --- NEW SANITIZATION STEP ---
        // 1. Remove anything that is NOT a letter (a-z), number (0-9), or space.
        //    The ^ inside [] means "Not". So [^a-zA-Z0-9\\s] means "Not alphanumeric or space".
        String cleanQuery = userQuery.replaceAll("[^a-zA-Z0-9\\s]", "");
        
        // 2. NOW add the OR pipes to the clean text
        //    Input: "Old times! bigbangtheory"
        //    Clean: "Old times bigbangtheory"
        //    Final: "Old | times | bigbangtheory"
        String formattedQuery = cleanQuery.trim().replaceAll("\\s+", " | ");

        // Check if the query became empty after cleaning (e.g. user entered "!!!")
        if (formattedQuery.isEmpty()) {
            return results;
        }

        // 3. The SQL remains the same
        String sql = "SELECT * FROM label WHERE to_tsvector('english', text_corrected) @@ to_tsquery('english', ?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, formattedQuery);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int number = rs.getInt("number");
                    String name = rs.getString("image_name");
                    String ocr = rs.getString("text_ocr");
                    String corrected = rs.getString("text_corrected");
                    
                    String sentimentStr = rs.getString("overall_sentiment");
                    Sentiment sentiment = null;
                    if (sentimentStr != null) {
                        sentiment = Sentiment.valueOf(sentimentStr);
                    }

                    results.add(new LabelRecord(number, name, ocr, corrected, sentiment));
                }
            }

        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}
