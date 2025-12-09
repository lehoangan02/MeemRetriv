package cat.dog.service;

import java.util.ArrayList;
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
        
        List<String> celebrities = new ArrayList<>();
        Object celebObj = processedResult.get("celebrities");
        if (celebObj instanceof List<?>) {
            for (Object item : (List<?>) celebObj) {
                if (item instanceof String) {
                    celebrities.add((String) item);
                }
            }
        }

        String caption = (String) processedResult.get("caption");
        System.out.println("Caption: " + caption);

        String text = (String) processedResult.get("text");

        
        return null;
    }
}
