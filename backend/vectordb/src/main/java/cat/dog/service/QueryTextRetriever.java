package cat.dog.service;

import java.util.List;
import java.util.Map;

import cat.dog.utility.LLMQueryProcessor;

public class QueryTextRetriever {
    private static QueryTextRetriever INSTANCE = new QueryTextRetriever();
    private QueryTextRetriever() {

    }
    public static QueryTextRetriever getInstance() {
        return INSTANCE;
    }
    public List<String> retrieveSimilarImages(String textQuery, int topK) {
        LLMQueryProcessor llmQueryProcessor = new LLMQueryProcessor();
        Map<String, Object> processedResult = llmQueryProcessor.processQuery(textQuery);
        // String 
        return null;
    }
}
