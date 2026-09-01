package team.themoment.hellogsmv3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HelloGsmV3Application {

    public static void main(String[] args) {
        SpringApplication.run(HelloGsmV3Application.class, args);
    }
}
