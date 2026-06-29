package team.themoment.hellogsmv3.global.security;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
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

    private static final String DEFAULT_COOKIE_NAME = "SESSION";
    private static final String DEFAULT_COOKIE_PATH = "/";
    private static final String SUBDOMAIN_COOKIE_PATTERN = "^(?:.+\\.)?(%s)$";

    private final ServerProperties serverProperties;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        boolean isDeployedEnv = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equals("dev") || p.equals("prod"));
        if (!isDeployedEnv)
            return;

        Cookie cookie = serverProperties.getServlet().getSession().getCookie();
        if (Cookie.SameSite.NONE == cookie.getSameSite() && !Boolean.TRUE.equals(cookie.getSecure())) {
            throw new IllegalStateException("SameSite=None 설정 시 Secure=true 가 필요합니다. 쿠키 Secure 설정을 true 로 변경하세요.");
        }
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        Cookie cookie = serverProperties.getServlet().getSession().getCookie();
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(StringUtils.hasText(cookie.getName()) ? cookie.getName() : DEFAULT_COOKIE_NAME);
        serializer.setCookiePath(StringUtils.hasText(cookie.getPath()) ? cookie.getPath() : DEFAULT_COOKIE_PATH);
        serializer.setUseHttpOnlyCookie(Boolean.TRUE.equals(cookie.getHttpOnly()));
        if (cookie.getSameSite() != null) {
            serializer.setSameSite(cookie.getSameSite().attributeValue());
        }
        if (cookie.getSecure() != null) {
            serializer.setUseSecureCookie(cookie.getSecure());
        }
        if (StringUtils.hasText(cookie.getDomain())) {
            serializer.setDomainNamePattern(SUBDOMAIN_COOKIE_PATTERN.formatted(Pattern.quote(cookie.getDomain())));
        }
        return serializer;
    }
}
