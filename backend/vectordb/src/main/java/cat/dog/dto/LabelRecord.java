package cat.dog.dto;

import cat.dog.model.Sentiment;

public class LabelRecord {
    private int number;
    private String imageName;
    private String imagePath;
    private String cleanedImagePath;
    private String textOcr;
    private String textCorrected;
    private Sentiment sentiment;

    public LabelRecord(int number, String imageName, String imagePath, String cleanedImagePath, String textOcr, String textCorrected, Sentiment sentiment) {
        this.number = number;
        this.imageName = imageName;
        this.imagePath = imagePath;
        this.cleanedImagePath = cleanedImagePath;
        this.textOcr = textOcr;
        this.textCorrected = textCorrected;
        this.sentiment = sentiment;
    }

    public int getNumber() { return number; }
    public String getImageName() { return imageName; }
    public String getImagePath() { return imagePath; }
    public String getCleanedImagePath() { return cleanedImagePath; }
    public String getTextOcr() { return textOcr; }
    public String getTextCorrected() { return textCorrected; }
    public Sentiment getSentiment() { return sentiment; }
}
