package cat.dog.dto;

public class MemeFaceRecord {
    private String memeName;
    private String filePath;

    public MemeFaceRecord(String memeName, String filePath) {
        this.memeName = memeName;
        this.filePath = filePath;
    }

    public String getMemeName() {
        return memeName;
    }

    public String getFilePath() {
        return filePath;
    }
}
