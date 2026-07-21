package team.themoment.hellogsmv3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableJpaAuditing
@SpringBootApplication
@EnableRedisHttpSession
public class HelloGsmV3Application {

    public static void main(String[] args) {
        SpringApplication.run(HelloGsmV3Application.class, args);
    }
}
