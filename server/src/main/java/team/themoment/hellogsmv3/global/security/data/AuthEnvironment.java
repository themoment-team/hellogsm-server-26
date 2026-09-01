package team.themoment.hellogsmv3.global.security.data;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthEnvironment(List<String> allowedOrigins) {
}
