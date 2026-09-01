package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import io.awspring.cloud.s3.S3Template;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrUploadUrlResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.data.S3Environment;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OCR 업로드 URL 발급 서비스 테스트")
class IssueOcrUploadUrlServiceTest {

    @Mock
    private AuthorizeOcrService authorizeOcrService;

    @Mock
    private S3Template s3Template;

    private IssueOcrUploadUrlService service;

    @BeforeEach
    void setUp() throws MalformedURLException {
        S3Environment s3Environment = new S3Environment("hello-test-bucket");
        service = new IssueOcrUploadUrlService(authorizeOcrService, s3Template, s3Environment);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("허용된 확장자가 주어진 경우")
        class Context_with_allowed_extension {

            @Test
            @DisplayName("rate limit을 확인한 뒤 presigned URL을 발급한다")
            void it_issues_presigned_url() throws MalformedURLException {
                URL signedUrl = URI.create("https://hello-test-bucket.s3.amazonaws.com/ocr-uploads/1/test.pdf").toURL();
                given(s3Template.createSignedPutURL(eq("hello-test-bucket"), any(), eq(Duration.ofMinutes(5))))
                        .willReturn(signedUrl);

                OcrUploadUrlResDto result = service.execute(1L, "pdf");

                verify(authorizeOcrService).execute(1L);
                assertThat(result.uploadUrl()).isEqualTo(signedUrl.toString());
                assertThat(result.objectKey()).startsWith("ocr-uploads/1/").endsWith(".pdf");
            }
        }

        @Nested
        @DisplayName("허용되지 않은 확장자가 주어진 경우")
        class Context_with_disallowed_extension {

            @Test
            @DisplayName("rate limit을 소모하지 않고 ExpectedException(400)을 던진다")
            void it_throws_bad_request() {
                assertThatThrownBy(() -> service.execute(1L, "exe")).isInstanceOf(ExpectedException.class)
                        .extracting(e -> ((ExpectedException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

                verify(authorizeOcrService, never()).execute(any());
                verify(s3Template, never()).createSignedPutURL(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("OCR 요청 횟수 제한을 초과한 경우")
        class Context_over_rate_limit {

            @Test
            @DisplayName("presigned URL을 발급하지 않고 예외를 전파한다")
            void it_propagates_exception_without_issuing_url() {
                ExpectedException rateLimitException = new ExpectedException("OCR 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                        HttpStatus.TOO_MANY_REQUESTS);
                org.mockito.BDDMockito.willThrow(rateLimitException).given(authorizeOcrService).execute(1L);

                assertThatThrownBy(() -> service.execute(1L, "pdf")).isSameAs(rateLimitException);

                verify(s3Template, never()).createSignedPutURL(any(), any(), any());
            }
        }
    }
}
