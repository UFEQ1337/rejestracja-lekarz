package pl.popielarz.rejestracja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RejestracjaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RejestracjaApplication.class, args);
    }
}
