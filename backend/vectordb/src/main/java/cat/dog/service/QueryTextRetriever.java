package cat.dog.service;

import cat.dog.dto.CelebEmbedding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.asm.Label;

import cat.dog.dto.CelebRecord;
import cat.dog.dto.LabelRecord;
import cat.dog.dto.MemeFaceRecord;
import cat.dog.repository.CelebFaceSearcher;
import cat.dog.repository.ElasticSearchDBManager;
import cat.dog.repository.WeviateExtractedFaceSearcher;
import cat.dog.repository.MemeSearcher;
import cat.dog.repository.PostgresDbManager;
import cat.dog.utility.LLMQueryProcessor;
import cat.dog.repository.ChromaExtractedFaceSearcher;

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

        long totalStart = System.currentTimeMillis();

        long start = System.currentTimeMillis();
        Map<String, Object> processedResult = llmQueryProcessor.processQuery(textQuery);
        long end = System.currentTimeMillis();
        System.out.println("LLM Processing took: " + (end - start) + " ms");

        start = System.currentTimeMillis();
        List<String> imageNamesFaceSearch = retrieveBaseOnFaceMatch(processedResult);
        end = System.currentTimeMillis();
        System.out.println("Face Search took: " + (end - start) + " ms");

        start = System.currentTimeMillis();
        List<String> imageNamesMemeCaptionSearch = retrieveBaseOnCaptionSearch(processedResult);
        end = System.currentTimeMillis();
        System.out.println("Caption Search took: " + (end - start) + " ms");

        start = System.currentTimeMillis();
        String text = (String) processedResult.get("text");
        List<String> imageNamesDescriptiveTextSearch = MemeSearcher.searchByText(text, "MemeImageCleaned", null);
        end = System.currentTimeMillis();

        start = System.currentTimeMillis();
        List<String> fallbackDescriptiveTextSearch = MemeSearcher.searchByText(textQuery, "MemeImage", null);
        end = System.currentTimeMillis();
        // combine both descriptive text search results
        System.out.println("Descriptive Text Search took: " + (end - start) + " ms");

        long totalEnd = System.currentTimeMillis();
        System.out.println("Total retrieveSimilarImages execution time: " + (totalEnd - totalStart) + " ms");

        // [Debug] Print intermediate results
        System.out.println("Face Search Results:");
        for (String imageName : imageNamesFaceSearch) {
            System.out.println(imageName);
        }
        System.out.println("Caption Search Results:");
        for (String imageName : imageNamesMemeCaptionSearch) {
            System.out.println(imageName);
        }
        System.out.println("Descriptive Text Search Results:");
        for (String imageName : imageNamesDescriptiveTextSearch) {
            System.out.println(imageName);
        }
        System.out.println("Fallback Descriptive Text Search Results:");
        for (String imageName : fallbackDescriptiveTextSearch) {
            System.out.println(imageName);
        }

        // if the text from processedResult is empty or contains only "a person", clear the descriptive text search results
        if (text == null || text.trim().isEmpty() || text.trim().equalsIgnoreCase("a person")) {
            imageNamesDescriptiveTextSearch.clear();
        }
        // if the caption from processedResult is empty or contains only "a person", clear the caption search results
        String caption = (String) processedResult.get("caption");
        if (caption == null || caption.trim().isEmpty() || caption.trim().equalsIgnoreCase("a person")) {
            imageNamesMemeCaptionSearch.clear();
        }

        // weighted merging of results
        float faceWeight = 0.2f;
        float captionWeight = 0.3f;
        float textWeight = 0.1f;
        float fallbackTextWeight = 0.2f;
        Map<String, Float> finalImageScores = new HashMap<>();
        for (int i = 0; i < imageNamesFaceSearch.size(); i++) {
            String imageName = imageNamesFaceSearch.get(i);
            float k = 30.0f;
            float score = faceWeight * (1.0f / (k + i + 1));
            boolean exists = finalImageScores.containsKey(imageName);
            if (!exists) {
                finalImageScores.put(imageName, 0.0f);
            }
            float accumulatedScore = finalImageScores.get(imageName) + score;
            finalImageScores.put(imageName, accumulatedScore);
        }
        for (int i = 0; i < imageNamesMemeCaptionSearch.size(); i++) {
            String imageName = imageNamesMemeCaptionSearch.get(i);
            float k = 30.0f;
            float score = captionWeight * (1.0f / (k + i + 1));
            boolean exists = finalImageScores.containsKey(imageName);
            if (!exists) {
                finalImageScores.put(imageName, 0.0f);
            }
            float accumulatedScore = finalImageScores.get(imageName) + score;
            finalImageScores.put(imageName, accumulatedScore);
        }
        for (int i = 0; i < imageNamesDescriptiveTextSearch.size(); i++) {
            String imageName = imageNamesDescriptiveTextSearch.get(i);
            float k = 30.0f;
            float score = textWeight * (1.0f / (k + i + 1));
            boolean exists = finalImageScores.containsKey(imageName);
            if (!exists) {
                finalImageScores.put(imageName, 0.0f);
            }
            float accumulatedScore = finalImageScores.get(imageName) + score;
            finalImageScores.put(imageName, accumulatedScore);
        }
        for (int i = 0; i < fallbackDescriptiveTextSearch.size(); i++) {
            String imageName = fallbackDescriptiveTextSearch.get(i);
            float k = 60.0f; // use a larger k to reduce the impact of fallback search
            float score = fallbackTextWeight * (1.0f / (k + i + 1));
            boolean exists = finalImageScores.containsKey(imageName);
            if (!exists) {
                finalImageScores.put(imageName, 0.0f);
            }
            float accumulatedScore = finalImageScores.get(imageName) + score;
            finalImageScores.put(imageName, accumulatedScore);
        }
        List<String> finalResults = finalImageScores.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .limit(topK).toList();
        
        // print final results
        System.out.println("Final Retrieved Images:");
        for (String imageName : finalResults) {
            System.out.println(imageName);
        }
        return finalResults;
    }
    private List<String> retrieveBaseOnCaptionSearch(Map<String,Object> processedResult) {
        PostgresDbManager pgManager = new PostgresDbManager();
        String caption = (String) processedResult.get("caption");
            // System.out.println("Caption: " + caption);
            ElasticSearchDBManager dbManager = ElasticSearchDBManager.getInstance();
            List<Map.Entry<Integer, String>> searchResults = dbManager.fuzzySearchCaptions(caption, 5.0f);
            // convert the integer reference id to image name stored in postgres
            List<String> imageNamesCaptionSearch = new ArrayList<>();
            
            for (Map.Entry<Integer, String> entry : searchResults) {
                LabelRecord record = pgManager.getRecordByNumber(entry.getKey());
                if (record != null) {
                    imageNamesCaptionSearch.add(record.getImageName());
                }
            }
        return imageNamesCaptionSearch;
    }
    private List<String> retrieveBaseOnFaceMatch(Map<String, Object> processedResult) {
        List<String> celebrities = new ArrayList<>();
        Object celebObj = processedResult.get("celebrities");
        if (celebObj instanceof List<?>) {
            for (Object item : (List<?>) celebObj) {
                if (item instanceof String) {
                    celebrities.add((String) item);
                }
            }
        }

        List<List<String>> allResults = new ArrayList<>();
        Map<String, Float> finalMemeScore = new HashMap<>();
        for (String celeb : celebrities) {
            // System.out.println("Celebrity: " + celeb);
            // Count time taken to search embeddings for this celebrity
            long startTime = System.currentTimeMillis();
            List<CelebEmbedding> celebEmbeddings = CelebFaceSearcher.getEmbeddingsByName(celeb);
            long endTime = System.currentTimeMillis();
            System.out.println("Time taken to search embeddings for " + celeb + ": " + (endTime - startTime) + " ms");
            //
            System.out.println("Found " + celebEmbeddings.size() + " embeddings for celebrity: " + celeb);
            List<List<MemeFaceRecord>> allFaces = new ArrayList<>();
            // Count time taken to search faces for all embeddings
            long faceSearchStartTime = System.currentTimeMillis();
            for (CelebEmbedding embedding : celebEmbeddings) {
                // System.out.println("Found embedding for: " + embedding.getCelebName() + " at " + embedding.getImagePath());
                // search similar faces in weaviate using the embedding vector
                List<MemeFaceRecord> faces = WeviateExtractedFaceSearcher.searchFaceWithEmbeddingByThreshold(embedding.getEmbedding(), 0.92f);
                // List<MemeFaceRecord> faces = ChromaExtractedFaceSearcher.searchFaceWithEmbedding(embedding.getEmbedding(), 10);
                allFaces.add(faces);
            }
            long faceSearchEndTime = System.currentTimeMillis();
            System.out.println("Time taken to search faces for all embeddings of " + celeb + ": " + (faceSearchEndTime - faceSearchStartTime) + " ms");
            //
            Map<String, Float> memeScore = new HashMap<>();
            for (List<MemeFaceRecord> faceList : allFaces) {
                for (MemeFaceRecord face : faceList) {
                    // check if memeName is already in memeScore
                    boolean exists = memeScore.containsKey(face.getMemeName());
                    if (!exists) {
                        memeScore.put(face.getMemeName(), 0.0f);
                    }
                }
            }
            for (List<MemeFaceRecord> faceList : allFaces) {
                for (int i = 0; i < faceList.size(); i++) {
                    float k = 30.0f;
                    float score = 1.0f / (k + i + 1);
                    MemeFaceRecord face = faceList.get(i);
                    float accumulatedScore = memeScore.get(face.getMemeName()) + score;
                    memeScore.put(face.getMemeName(), accumulatedScore);
                }
            }
            List<String> singleCelebResult = memeScore.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .toList();

            allResults.add(singleCelebResult);
        }
        for (List<String> resultList : allResults) {
            for (int i = 0; i < resultList.size(); i++) {
                String memeName = resultList.get(i);
                float k = 30.0f;
                float score = 1.0f / (k + i + 1);
                boolean exists = finalMemeScore.containsKey(memeName);
                if (!exists) {
                    finalMemeScore.put(memeName, 0.0f);
                }
                float accumulatedScore = finalMemeScore.get(memeName) + score;
                finalMemeScore.put(memeName, accumulatedScore);
            }
        }
        List<String> finalResults = finalMemeScore.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        return finalResults;
    }
    public static void main(String[] args) {
        QueryTextRetriever retriever = QueryTextRetriever.getInstance();
        List<String> results = retriever.retrieveSimilarImages("A funny meme about cats and dogs", 5);
    }
    private class CelebImageEmbedding {
        public String celebName;
        public String imagePath;
        public String[] embedding;
    }
}
