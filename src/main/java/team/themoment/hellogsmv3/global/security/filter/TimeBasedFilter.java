package team.themoment.hellogsmv3.global.security.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;
import team.themoment.sdk.response.CommonApiResponse;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class TimeBasedFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final List<RequestPeriod> requestPeriods = new ArrayList<>();

    private record RequestPeriod(HttpMethod method, PathPattern pattern, LocalDateTime startTime,
            LocalDateTime endTime) {
        boolean matches(HttpMethod requestMethod, String requestUri) {
            return method.equals(requestMethod) && pattern.matches(PathContainer.parsePath(requestUri));
        }

        boolean isWithinRange(LocalDateTime currentTime) {
            return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
        }
    }

    public static TimeBasedFilter from(ScheduleEnvironment schedule, ObjectMapper objectMapper) {
        return new TimeBasedFilter(objectMapper)
                .addFilter(HttpMethod.POST,
                        "/oneseo/v3/temp-storage",
                        schedule.oneseoSubmissionStart(),
                        schedule.oneseoSubmissionEnd())
                .addFilter(HttpMethod.POST,
                        "/oneseo/v3/oneseo/me",
                        schedule.oneseoSubmissionStart(),
                        schedule.oneseoSubmissionEnd())
                .addFilter(HttpMethod.POST,
                        "/oneseo/v3/image",
                        schedule.oneseoSubmissionStart(),
                        schedule.oneseoSubmissionEnd())
                .addFilter(HttpMethod.POST,
                        "/oneseo/v3/excel",
                        schedule.interview(),
                        schedule.finalResultsAnnouncement());
    }

    /** 등록 순서대로 검사하며 처음 일치한 규칙의 기간을 적용합니다. */
    public TimeBasedFilter addFilter(HttpMethod httpMethod,
            String uri,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        requestPeriods
                .add(new RequestPeriod(httpMethod, PathPatternParser.defaultInstance.parse(uri), startTime, endTime));
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

        RequestPeriod requestPeriod = requestPeriods.stream()
                .filter(period -> period.matches(requestMethod, requestUri)).findFirst().orElse(null);
        if (requestPeriod != null) {

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
