package cat.dog.dto;

public class Base64ImageResponse extends Base64Image {
    private String sentiment;

    public Base64ImageResponse(String filename, String imageBase64, String sentiment) {
        super(filename, imageBase64);
        this.sentiment = sentiment;
    }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
}
