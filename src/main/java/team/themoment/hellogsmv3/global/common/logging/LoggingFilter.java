package team.themoment.hellogsmv3.global.common.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private static final String[] NOT_LOGGING_URL = {"/api-docs/**", "/swagger-ui/**",
            "/hello-management/prometheus/**"};

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {

        if (isNotLoggingURL(request.getRequestURI())) {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                log.error("로깅 제외 경로 예외", e);
            }

            return;
        }

        // 멀티파트 요청은 바디 로깅/캐싱 생략, 메타데이터만 기록
        if (isMultipart(request)) {
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            UUID logId = UUID.randomUUID();
            long startTime = System.currentTimeMillis();
            try {
                requestLoggingMultipart(request, logId);
                filterChain.doFilter(request, responseWrapper);
            } catch (Exception e) {
                log.error("LoggingFilter의 FilterChain에서 예외가 발생했습니다.", e);
            } finally {
                responseLogging(responseWrapper, startTime, logId);
                try {
                    responseWrapper.copyBodyToResponse();
                } catch (IOException e) {
                    log.error("LoggingFilter에서 response body를 출력하는 도중 예외가 발생했습니다.", e);
                }
            }
            return;
        }

        CachedBodyRequestWrapper cachedRequest;
        try {
            cachedRequest = new CachedBodyRequestWrapper(request);
        } catch (IOException e) {
            log.error("요청 바디 캐싱 중 예외 발생 - 원본 요청으로 진행합니다.", e);
            try {
                filterChain.doFilter(request, response);
            } catch (Exception ex) {
                log.error("LoggingFilter의 FilterChain에서 예외가 발생했습니다.", ex);
            }
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        UUID logId = UUID.randomUUID();
        long startTime = System.currentTimeMillis();

        try {
            requestLogging(cachedRequest, logId, cachedRequest.getCachedBody());
            filterChain.doFilter(cachedRequest, responseWrapper);
        } catch (Exception e) {
            log.error("LoggingFilter의 FilterChain에서 예외가 발생했습니다.", e);
        } finally {
            responseLogging(responseWrapper, startTime, logId);
            try {
                responseWrapper.copyBodyToResponse();
            } catch (IOException e) {
                log.error("LoggingFilter에서 response body를 출력하는 도중 예외가 발생했습니다.", e);
            }
        }
    }

    private boolean isNotLoggingURL(String requestURI) {
        return Arrays.stream(NOT_LOGGING_URL).anyMatch(pattern -> matcher.match(pattern, requestURI));
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private void requestLogging(HttpServletRequest request, UUID logId, byte[] cachedBody) {
        log.info(
                "Log-ID: {}, IP: {}, URI: {}, Http-Method: {}, Params: {}, Content-Type: {}, User-Cookies: {}, User-Agent: {}, Request-Body: {}",
                logId,
                request.getRemoteAddr(),
                request.getRequestURI(),
                request.getMethod(),
                request.getQueryString(),
                request.getContentType(),
                request.getCookies() != null ? String.join(", ", getCookiesAsString(request.getCookies())) : "[none]",
                request.getHeader("User-Agent"),
                getRequestBody(cachedBody));
    }

    private void requestLoggingMultipart(HttpServletRequest request, UUID logId) {
        String contentLength = request.getHeader("Content-Length");
        log.info(
                "Log-ID: {}, IP: {}, URI: {}, Http-Method: {}, Params: {}, Content-Type: {}, Content-Length: {}, User-Cookies: {}, User-Agent: {}, Request-Body: {}",
                logId,
                request.getRemoteAddr(),
                request.getRequestURI(),
                request.getMethod(),
                request.getQueryString(),
                request.getContentType(),
                contentLength != null ? contentLength : "[unknown]",
                request.getCookies() != null ? String.join(", ", getCookiesAsString(request.getCookies())) : "[none]",
                request.getHeader("User-Agent"),
                "[multipart omitted]");
    }

    private void responseLogging(ContentCachingResponseWrapper response, long startTime, UUID logId) {
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        log.info("Log-ID: {}, Status-Code: {}, Content-Type: {}, Response Time: {}ms, Response-Body: {}",
                logId,
                response.getStatus(),
                response.getContentType(),
                responseTime,
                new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private String getRequestBody(byte[] byteArrayContent) {
        String oneLineContent = new String(byteArrayContent, StandardCharsets.UTF_8).replaceAll("\\s", "");
        return StringUtils.hasText(oneLineContent) ? oneLineContent : "[empty]";
    }

    private String[] getCookiesAsString(Cookie[] cookies) {
        return Arrays.stream(cookies).map(cookie -> String.format("%s=%s", cookie.getName(), cookie.getValue()))
                .toArray(String[]::new);
    }
}
