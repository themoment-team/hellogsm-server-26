package team.themoment.hellogsmv3.global.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Spring Session(Redis) 기반 세션 쿠키 설정.
 *
 * <p>
 * {@code @EnableRedisHttpSession} 을 사용하면 Spring Boot 의 쿠키 직렬화 자동 구성이 비활성화되어
 * {@code server.servlet.session.cookie.*} 프로퍼티가 세션 쿠키에 반영되지 않는다. 따라서
 * 도메인/SameSite/Secure 를 실제로 적용하려면 {@link CookieSerializer} 빈을 직접 등록해야 한다.
 */
@Configuration
@EnableRedisHttpSession
@RequiredArgsConstructor
public class SessionConfig {

    private static final String COOKIE_NAME = "SESSION";
    private static final String COOKIE_PATH = "/";

    private final Environment environment;

    @Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;

    @Value("${server.servlet.session.cookie.same-site:lax}")
    private String sameSite;

    @Value("${server.servlet.session.cookie.secure:false}")
    private boolean secure;

    @PostConstruct
    public void validate() {
        boolean isDeployedEnv = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equals("dev") || p.equals("prod"));
        if (!isDeployedEnv)
            return;

        if (Cookie.SameSite.NONE == Cookie.SameSite.valueOf(sameSite.toUpperCase()) && !secure) {
            throw new IllegalStateException("SameSite=None 설정 시 Secure=true 가 필요합니다. 쿠키 Secure 설정을 true 로 변경하세요.");
        }
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(COOKIE_NAME);
        serializer.setCookiePath(COOKIE_PATH);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite(sameSite);
        serializer.setUseSecureCookie(secure);
        if (StringUtils.hasText(cookieDomain)) {
            serializer.setDomainName(cookieDomain);
        }
        return serializer;
    }
}
