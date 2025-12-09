package cat.dog.service;

import java.util.List;

public class QueryTextRetriever {
    private static QueryTextRetriever INSTANCE = new QueryTextRetriever();
    private QueryTextRetriever() {

    }
    public static QueryTextRetriever getInstance() {
        return INSTANCE;
    }
    public List<String> retrieveSimilarImages(String textQuery, int topK) {
        return null;
    }
}
