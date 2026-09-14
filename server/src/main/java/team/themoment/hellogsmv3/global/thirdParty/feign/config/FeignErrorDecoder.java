package team.themoment.hellogsmv3.global.thirdParty.feign.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import team.themoment.sdk.exception.ExpectedException;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        if (status >= 400) {
            String errorBody = extractErrorBody(response);
            Map<String, Collection<String>> headers = response.headers();
            String url = response.request().url();
            String httpMethod = response.request().httpMethod().name();
            log.error("Feign 클라이언트 오류 - 메서드: {}, HTTP 메서드: {}, URL: {}, 상태: {}, 이유: {}",
                    methodKey,
                    httpMethod,
                    url,
                    status,
                    response.reason());
            log.error("응답 헤더: {}", headers);
            log.error("응답 본문: {}", errorBody);
            logRequestDetails(response, methodKey);
            String userMessage;
            HttpStatus httpStatus;
            switch (status) {
                case 400 -> {
                    userMessage = "잘못된 요청입니다.";
                    httpStatus = HttpStatus.BAD_REQUEST;
                }
                case 401 -> {
                    userMessage = "인증이 필요합니다.";
                    httpStatus = HttpStatus.UNAUTHORIZED;
                }
                case 403 -> {
                    userMessage = "접근이 거부되었습니다.";
                    httpStatus = HttpStatus.FORBIDDEN;
                }
                case 404 -> {
                    userMessage = "요청하신 리소스를 찾을 수 없습니다.";
                    httpStatus = HttpStatus.NOT_FOUND;
                }
                case 429 -> {
                    userMessage = "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
                    httpStatus = HttpStatus.TOO_MANY_REQUESTS;
                }
                case 500 -> {
                    userMessage = "외부 서비스 내부 오류가 발생했습니다.";
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                }
                case 502 -> {
                    userMessage = "게이트웨이 오류가 발생했습니다.";
                    httpStatus = HttpStatus.BAD_GATEWAY;
                }
                case 503 -> {
                    userMessage = "서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.";
                    httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
                }
                default -> {
                    userMessage = "외부 요청 처리 중 오류가 발생했습니다.";
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                }
            }
            throw new ExpectedException(userMessage, httpStatus);
        }
        return FeignException.errorStatus(methodKey, response);
    }

    private String extractErrorBody(Response response) {
        try {
            if (response.body() != null) {
                return StreamUtils.copyToString(response.body().asInputStream(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("오류 응답 본문을 읽는 데 실패했습니다", e);
        }
        return "응답 본문을 읽을 수 없습니다";
    }

    private void logRequestDetails(Response response, String methodKey) {
        try {
            String url = response.request().url();
            String method = response.request().httpMethod().name();
            Map<String, Collection<String>> requestHeaders = response.request().headers();
            log.error("요청 정보 - 메서드: {}, HTTP 메서드: {}, URL: {}", methodKey, method, url);
            log.error("요청 헤더: {}", requestHeaders);
            if (response.request().body() != null) {
                String requestBody = new String(response.request().body(), StandardCharsets.UTF_8);
                log.error("요청 본문: {}", requestBody);
            }
        } catch (Exception e) {
            log.warn("요청 상세 로깅에 실패했습니다", e);
        }
    }
}
