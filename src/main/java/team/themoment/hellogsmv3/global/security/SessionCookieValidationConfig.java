package team.themoment.hellogsmv3.global.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SessionCookieValidationConfig {

    private final Environment environment;

    @Value("${COOKIE_DOMAIN:}")
    private String cookieDomain;

    @Value("${server.servlet.session.cookie.same-site:lax}")
    private String sameSite;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secure;

    @PostConstruct
    public void validate() {
        boolean isDeployedEnv = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(p -> p.equals("dev") || p.equals("prod"));
        if (!isDeployedEnv) return;

        if (Cookie.SameSite.NONE == Cookie.SameSite.valueOf(sameSite.toUpperCase()) && !secure) {
            throw new IllegalStateException(
                "SameSite=None 설정 시 Secure=true 가 필요합니다. COOKIE_SECURE 환경 변수를 true 로 설정하세요."
            );
        }
    }

    @Bean
    public ServletContextInitializer sessionCookieDomainInitializer() {
        return servletContext -> {
            if (StringUtils.hasText(cookieDomain)) {
                servletContext.getSessionCookieConfig().setDomain(cookieDomain);
            }
        };
    }
}
