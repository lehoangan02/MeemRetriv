package cat.dog.dto;

public class CelebEmbedding {
    private String celebName;
    private String imagePath;
    private String[] embedding;

    public CelebEmbedding(String celebName, String imagePath, String[] embedding) {
        this.celebName = celebName;
        this.imagePath = imagePath;
        this.embedding = embedding;
    }

    public String getCelebName() { return celebName; }
    public String getImagePath() { return imagePath; }
    public String[] getEmbedding() { return embedding; }
}
