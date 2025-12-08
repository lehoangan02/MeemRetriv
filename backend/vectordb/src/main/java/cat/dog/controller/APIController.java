package cat.dog.controller;

import cat.dog.dto.Base64Image;
import cat.dog.repository.LabelDbManager;
import cat.dog.service.QueryImageRetriever;
import cat.dog.utility.Base64ImageConverter;
import cat.dog.dto.Base64ImageResponse;
import cat.dog.dto.LabelRecord;
import tools.jackson.core.ObjectReadContext.Base;
import javax.management.Query;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class APIController {
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "!\n";
    }
    @PostMapping("/uploadImageBase64")
    public ResponseEntity<List<Base64ImageResponse>> uploadImageBase64(@RequestBody Base64Image request) {
        boolean success = Base64ImageConverter.saveBase64AsPng(request.getImageBase64());
        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);  // failed to decode image
        }
        QueryImageRetriever retriever = QueryImageRetriever.getInstance();
        List<String> results = retriever.retrieveSimilarImages("./received_images/query_image.png", 40);
        List<String> resultsBase64 = new java.util.ArrayList<>();
        List<String> sentiments = new java.util.ArrayList<>();
        List<String> filePaths = new java.util.ArrayList<>();
        LabelDbManager labelDbManager = new LabelDbManager();
        for (String res : results) {
            LabelRecord record = labelDbManager.getRecordByImageName(res);
            String imagePath = record.getImagePath();
            filePaths.add(imagePath);
            String sentiment = record.getSentiment().toDbValue();
            String base64Image = Base64ImageConverter.convertToBase64(imagePath);
            resultsBase64.add(base64Image);
            sentiments.add(sentiment);
        }
        List<Base64ImageResponse> responseList = new java.util.ArrayList<>();
        for (int i = 0; i < resultsBase64.size(); i++) {
            responseList.add(new Base64ImageResponse(filePaths.get(i), resultsBase64.get(i), sentiments.get(i)));
        }
        System.out.println("Returning " + responseList.size() + " images.");
        return ResponseEntity.status(HttpStatus.OK)
                .body(responseList);
    }
}
