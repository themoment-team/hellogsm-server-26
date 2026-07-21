package team.themoment.hellogsmv3.global.config;

import java.time.LocalDateTime;
import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void init() {
        try {
            System.setProperty("user.timezone", "Asia/Seoul");
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
            log.info("Default timezone set to Asia/Seoul: {}", LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Failed to set default timezone: ", e);
        }
    }
}
