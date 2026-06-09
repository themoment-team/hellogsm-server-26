package team.themoment.hellogsmv3.global.security.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import team.themoment.sdk.response.CommonApiResponse;

@Slf4j
public class TimeBasedFilter extends OncePerRequestFilter {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, RequestPeriod> urlRequestPeriods = new HashMap<>();

    private record RequestPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        public boolean isWithinRange(LocalDateTime currentTime) {
            return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
        }
    }

    public TimeBasedFilter addFilter(HttpMethod httpMethod,
            String uri,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        urlRequestPeriods.put(uri + ":" + httpMethod, new RequestPeriod(startTime, endTime));
        return this;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        HttpMethod requestMethod;

        try {
            requestMethod = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않은 HTTP 메서드 입니다. : " + request.getMethod());
            return;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        String requestPeriodKey = requestUri + ":" + requestMethod;

        if (urlRequestPeriods.containsKey(requestPeriodKey)) {
            RequestPeriod requestPeriod = urlRequestPeriods.get(requestPeriodKey);

            if (requestPeriod.isWithinRange(currentTime)) {
                filterChain.doFilter(request, response);
            } else {
                String message = String.format("%s 요청이 거부되었습니다. " + "현재 시간: %s, 해당 요청은 %s ~ %s 이내에만 처리 가능합니다.",
                        requestPeriodKey,
                        currentTime,
                        requestPeriod.startTime,
                        requestPeriod.endTime);
                log.warn(message);
                sendErrorResponse(response, message);
            }

            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(CommonApiResponse.error(message, HttpStatus.FORBIDDEN)));
    }
}
