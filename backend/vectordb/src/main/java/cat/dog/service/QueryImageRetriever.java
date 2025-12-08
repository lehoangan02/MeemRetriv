package cat.dog.service;

import cat.dog.repository.MemeSearcher;
import cat.dog.utility.ClipEmbedder ;
import cat.dog.utility.RemoveMemeText;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class QueryImageRetriever {
    private static final QueryImageRetriever INSTANCE = new QueryImageRetriever();
    private QueryImageRetriever() {

    }
    public static QueryImageRetriever getInstance() {
        return INSTANCE;
    }
    public List<String> retrieveSimilarImages(String imagePath, int topK) {
        
        List<String> results = MemeSearcher.searchByImage(imagePath, "MemeImage", null);
        RemoveMemeText.clean("./received_images/query_image.png", "./received_images/cleaned_query_image.png");
        List<String> cleanedResults = MemeSearcher.searchByImage("./received_images/cleaned_query_image.png", "MemeImageCleaned", null);

        float originalWeight = 0.6f;
        float cleanedWeight = 0.4f;

        Map<String, Float> scoreMap = new HashMap<>();
        
        // set all results score to 0
        for (String res : results) {
            scoreMap.put(res, 0.0f);
        }
        for (String res : cleanedResults) {
            scoreMap.put(res, 0.0f);
        }

        // accumulate scores = ∑ (weight * rank_score)
        // rank_score = 1 / (k + rank_index)

        float k = 30.0f;
        for (int i = 0; i < results.size(); i++) {
            String res = results.get(i);
            float rankScore = 1.0f / (k + i);
            float accumulatedScore = scoreMap.get(res) + originalWeight * rankScore;
            scoreMap.put(res, accumulatedScore);
        }
        for (int i = 0; i < cleanedResults.size(); i++) {
            String res = cleanedResults.get(i);
            float rankScore = 1.0f / (k + i);
            float accumulatedScore = scoreMap.get(res) + cleanedWeight * rankScore;
            scoreMap.put(res, accumulatedScore);
        }

        // print all scores in the map
        for (Map.Entry<String, Float> entry : scoreMap.entrySet()) {
            System.out.println("Image: " + entry.getKey() + ", Score: " + entry.getValue() + "\n");
        }
        // sort by score descending
        List<String> finalResults = scoreMap.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        // print topK final results
        System.out.println("Top " + topK + " final results:");
        for (int i = 0; i < Math.min(topK, finalResults.size()); i++) {
            System.out.println("Image: " + finalResults.get(i) + ", Score: " + scoreMap.get(finalResults.get(i)) + "\n");
        }
        return finalResults.subList(0, Math.min(topK, finalResults.size()));
    }
}
