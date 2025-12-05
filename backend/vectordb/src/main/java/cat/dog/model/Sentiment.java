package cat.dog.model;

public enum Sentiment {
    very_positive,
    positive,
    neutral,
    negative,
    very_negative;

    public String toDbValue() {
        return this.name();
    }
}
