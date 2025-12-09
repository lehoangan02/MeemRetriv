package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import cat.dog.utility.DatabaseConfig;

public class WeviateSchemaWiper {

    private final String baseUrl;
    private final HttpClient client;

    public WeviateSchemaWiper() {
        this.baseUrl = DatabaseConfig.getInstance().getWeviateUrl() + "/schema/";
        this.client = HttpClient.newHttpClient();
    }

    public void deleteClass(String className) throws Exception {
        String deleteUrl = baseUrl + className;
        System.out.println("Full delete URL: " + deleteUrl);

        System.out.println("Deleting schema class: " + className);
        System.out.println("URL: " + deleteUrl);

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Delete status: " + deleteResponse.statusCode());
        System.out.println(deleteResponse.body());
    }

    public static void main(String[] args) throws Exception {
        String class1 = "MemeImage";
        String class2 = "MemeImageCleaned";
        WeviateSchemaWiper wiper = new WeviateSchemaWiper();
        wiper.deleteClass(class1);
        wiper.deleteClass(class2);
    }
}
