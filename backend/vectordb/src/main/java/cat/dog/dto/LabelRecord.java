package cat.dog.dto;

import cat.dog.model.Sentiment;

public class LabelRecord {
    private int number;
    private String imageName;
    private String textOcr;
    private String textCorrected;
    private Sentiment sentiment;

    public LabelRecord(int number, String imageName, String textOcr, String textCorrected, Sentiment sentiment) {
        this.number = number;
        this.imageName = imageName;
        this.textOcr = textOcr;
        this.textCorrected = textCorrected;
        this.sentiment = sentiment;
    }

    public int getNumber() { return number; }
    public String getImageName() { return imageName; }
    public String getTextOcr() { return textOcr; }
    public String getTextCorrected() { return textCorrected; }
    public Sentiment getSentiment() { return sentiment; }
}
