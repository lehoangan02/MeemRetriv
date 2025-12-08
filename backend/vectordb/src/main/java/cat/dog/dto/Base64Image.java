package cat.dog.dto;

public class Base64Image {
    private String filename;
    private String imageBase64;

    public Base64Image(String filename, String imageBase64) {
        this.filename = filename;
        this.imageBase64 = imageBase64;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}
