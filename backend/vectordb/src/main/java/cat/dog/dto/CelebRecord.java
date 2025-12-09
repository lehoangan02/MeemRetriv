package cat.dog.dto;

public class CelebRecord {
    private String imagePath;
    private String celebName;
    private int classifiedInteger;

    public CelebRecord(String imagePath, String celebName, int classifiedInteger) {
        this.imagePath = imagePath;
        this.celebName = celebName;
        this.classifiedInteger = classifiedInteger;
    }

    public String getImagePath() { return imagePath; }
    public String getCelebName() { return celebName; }
    public int getClassifiedInteger() { return classifiedInteger; }
}
