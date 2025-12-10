package cat.dog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.asm.Label;

import cat.dog.dto.CelebRecord;
import cat.dog.dto.LabelRecord;
import cat.dog.repository.ElasticSearchDBManager;
import cat.dog.repository.MemeSearcher;
import cat.dog.repository.PostgresDbManager;
import cat.dog.utility.LLMQueryProcessor;

public class QueryTextRetriever {
    private static QueryTextRetriever INSTANCE = new QueryTextRetriever();
    private QueryTextRetriever() {

    }
    public static QueryTextRetriever getInstance() {
        return INSTANCE;
    }
    public List<String> retrieveSimilarImages(String textQuery, int topK) {
        PostgresDbManager pgManager = new PostgresDbManager();
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

        for (String celeb : celebrities) {
            System.out.println("Celebrity: " + celeb);
            // get images of that celebrity from postgres
            List<CelebRecord> celebRecords = pgManager.searchCelebByName(celeb);
            for (CelebRecord record : celebRecords) {
                
            }
        }

        String caption = (String) processedResult.get("caption");
        System.out.println("Caption: " + caption);
        ElasticSearchDBManager dbManager = ElasticSearchDBManager.getInstance();
        List<Map.Entry<Integer, String>> searchResults = dbManager.fuzzySearchCaptions(textQuery, 5.0f);
        // convert the integer reference id to image name stored in postgres
        List<String> imageNamesCaptionSearch = new ArrayList<>();
        
        for (Map.Entry<Integer, String> entry : searchResults) {
            LabelRecord record = pgManager.getRecordByNumber(entry.getKey());
            if (record != null) {
                imageNamesCaptionSearch.add(record.getImageName());
            }
        }

        String text = (String) processedResult.get("text");
        List<String> imageNamesDescriptiveTextSearch = MemeSearcher.searchByText(text, "MemeImageCleaned", null);
        
        return null;
    }
    public static void main(String[] args) {
        QueryTextRetriever retriever = QueryTextRetriever.getInstance();
        List<String> results = retriever.retrieveSimilarImages("A funny meme about cats and dogs", 5);
    }
}
