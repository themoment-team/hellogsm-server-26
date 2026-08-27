package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OCR 실행 인증 서비스 테스트")
class AuthorizeOcrServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthorizeOcrService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizeOcrService(redisTemplate);
        ReflectionTestUtils.setField(service, "maxRequests", 3);
        ReflectionTestUtils.setField(service, "windowMinutes", 10L);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("회원의 최근 요청 횟수가 한도 이내인 경우")
        class Context_within_limit {

            @Test
            @DisplayName("예외를 던지지 않는다")
            void it_does_not_throw() {
                given(redisTemplate.opsForValue()).willReturn(valueOperations);
                given(valueOperations.increment("ocr-rate-limit:1")).willReturn(2L);

                service.execute(1L);

                verify(redisTemplate, never()).expire("ocr-rate-limit:1", Duration.ofMinutes(10));
            }
        }

        @Nested
        @DisplayName("이번 요청이 해당 회원의 첫 요청인 경우")
        class Context_first_request {

            @Test
            @DisplayName("만료 시간을 설정한다")
            void it_sets_expiration() {
                given(redisTemplate.opsForValue()).willReturn(valueOperations);
                given(valueOperations.increment("ocr-rate-limit:1")).willReturn(1L);

                service.execute(1L);

                verify(redisTemplate).expire("ocr-rate-limit:1", Duration.ofMinutes(10));
            }
        }

        @Nested
        @DisplayName("회원의 최근 요청 횟수가 한도를 초과한 경우")
        class Context_over_limit {

            @Test
            @DisplayName("ExpectedException(429)을 던진다")
            void it_throws_too_many_requests() {
                given(redisTemplate.opsForValue()).willReturn(valueOperations);
                given(valueOperations.increment("ocr-rate-limit:1")).willReturn(4L);

                assertThatThrownBy(() -> service.execute(1L)).isInstanceOf(ExpectedException.class)
                        .extracting(e -> ((ExpectedException) e).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            }
        }
    }
}
