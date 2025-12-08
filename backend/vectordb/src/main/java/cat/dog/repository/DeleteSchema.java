package cat.dog.repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import cat.dog.utility.DatabaseConfig;

public class DeleteSchema {

    public static void main(String[] args) throws Exception {

        final String BASE_URL = DatabaseConfig.getInstance().getWeviateUrl() + "/classes/";

        if (args.length == 0) {
            System.out.println("Usage: java cat.dog.repository.DeleteSchema <ClassName>");
            return;
        }

        String className = args[0];
        String deleteUrl = BASE_URL + className;

        System.out.println("Deleting schema class: " + className);
        System.out.println("URL: " + deleteUrl);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("Delete status: " + deleteResponse.statusCode());
        System.out.println(deleteResponse.body());
    }
}
