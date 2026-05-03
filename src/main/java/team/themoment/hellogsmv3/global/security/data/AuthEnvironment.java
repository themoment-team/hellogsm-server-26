package team.themoment.hellogsmv3.global.security.data;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthEnvironment(List<String> allowedOrigins, @NotNull List<String> allowedRedirectUris) {
}
