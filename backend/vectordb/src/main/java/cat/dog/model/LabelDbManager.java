package cat.dog.model;

import cat.dog.dto.LabelRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

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
}
