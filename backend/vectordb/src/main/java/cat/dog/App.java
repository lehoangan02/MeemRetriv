package cat.dog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
public class App 
{
    public static void main( String[] args )
    {
        System.out.println("Starting");
        SpringApplication.run(App.class, args);
    }
}
