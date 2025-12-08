package cat.dog.service;

import cat.dog.repository.MemeSearcher;
import cat.dog.utility.ClipEmbedder ;

import java.util.List;

public class QueryImageRetriever {
    private static final QueryImageRetriever INSTANCE = new QueryImageRetriever();
    private QueryImageRetriever() {

    }
    public static QueryImageRetriever getInstance() {
        return INSTANCE;
    }
    public List<String> retrieveSimilarImages(String imagePath, int topK) {
        
        List<String> results = MemeSearcher.searchByImage(imagePath, "MemeImage", null);

        return null;
    }
}
