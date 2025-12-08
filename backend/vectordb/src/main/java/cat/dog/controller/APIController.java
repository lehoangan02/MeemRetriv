package cat.dog.controller;

import cat.dog.dto.Base64Image;
import cat.dog.utility.Base64ToImageConverter;

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
    public ResponseEntity<String> uploadImageBase64(@RequestBody Base64Image request) {
        boolean success = Base64ToImageConverter.saveBase64AsPng(request.getImageBase64());
        if (success) {
            return ResponseEntity.ok("Image saved as received_images/query_image.png");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image data");
        }
    }
}
