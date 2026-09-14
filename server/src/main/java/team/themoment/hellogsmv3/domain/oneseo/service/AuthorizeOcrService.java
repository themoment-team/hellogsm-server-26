package team.themoment.hellogsmv3.domain.oneseo.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.sdk.exception.ExpectedException;

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
        Long ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl < 0) {
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }

        if (count != null && count > maxRequests) {
            throw new ExpectedException("OCR 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
