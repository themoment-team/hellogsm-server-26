package team.themoment.hellogsmv3.domain.oneseo.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.sdk.exception.ExpectedException;

/**
 * 프론트(Next.js API Route)가 kordoc OCR을 실행하기 직전에 호출합니다. 로그인 여부는 이 엔드포인트 자체가 이미
 * {@code APPLICANT_OR_ROOT} 권한을 요구하므로 Spring Security가 먼저 걸러주고, 여기서는 회원별 OCR 요청
 * 횟수만 확인합니다.
 *
 * <p>
 * OCR 실행 자체는 프론트(Vercel)에서 이루어지므로, 여기서 막는 것은 "OCR을 실행해도 되는지"에 대한 허가일 뿐 실제 처리량을
 * 제어하지는 않습니다 — 프론트가 이 허가를 받은 뒤에만 OCR을 실행하는 것을 전제로 합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthorizeOcrService {

    private static final String REDIS_KEY_PREFIX = "ocr-rate-limit:";

    private final StringRedisTemplate redisTemplate;

    @Value("${oneseo.extraction.ocr-rate-limit.max-requests:30}")
    private int maxRequests;

    @Value("${oneseo.extraction.ocr-rate-limit.window-minutes:10}")
    private long windowMinutes;

    public void execute(Long memberId) {
        String key = REDIS_KEY_PREFIX + memberId;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }

        if (count != null && count > maxRequests) {
            throw new ExpectedException("OCR 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
