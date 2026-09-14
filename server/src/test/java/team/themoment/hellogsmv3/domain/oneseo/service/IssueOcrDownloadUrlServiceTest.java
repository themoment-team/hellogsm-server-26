package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrDownloadUrlResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.data.S3Environment;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OCR 다운로드 URL 발급 서비스 테스트")
class IssueOcrDownloadUrlServiceTest {

    @Mock
    private S3Template s3Template;

    private IssueOcrDownloadUrlService service;

    @BeforeEach
    void setUp() {
        S3Environment s3Environment = new S3Environment("hello-test-bucket");
        service = new IssueOcrDownloadUrlService(s3Template, s3Environment);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("요청자 소유의 objectKey이고 파일이 존재하며 용량이 허용 범위인 경우")
        class Context_with_owned_existing_and_valid_size_object_key {

            @Test
            @DisplayName("presigned GET URL을 발급한다")
            void it_issues_presigned_url() throws MalformedURLException {
                String objectKey = "ocr-uploads/1/test.pdf";
                URL signedUrl = URI.create("https://hello-test-bucket.s3.amazonaws.com/" + objectKey).toURL();
                S3Resource resource = mock(S3Resource.class);
                given(resource.exists()).willReturn(true);
                given(resource.contentLength()).willReturn(10L * 1024 * 1024);
                given(s3Template.createResource("hello-test-bucket", objectKey)).willReturn(resource);
                given(s3Template.createSignedGetURL(eq("hello-test-bucket"), eq(objectKey), eq(Duration.ofMinutes(1))))
                        .willReturn(signedUrl);

                OcrDownloadUrlResDto result = service.execute(1L, objectKey);

                assertThat(result.downloadUrl()).isEqualTo(signedUrl.toString());
            }
        }

        @Nested
        @DisplayName("다른 회원 소유의 objectKey가 주어진 경우")
        class Context_with_not_owned_object_key {

            @Test
            @DisplayName("ExpectedException(403)을 던진다")
            void it_throws_forbidden() {
                String objectKey = "ocr-uploads/2/test.pdf";

                assertThatThrownBy(() -> service.execute(1L, objectKey)).isInstanceOf(ExpectedException.class)
                        .extracting(e -> ((ExpectedException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

                verify(s3Template, never()).createResource(any(), any());
                verify(s3Template, never()).createSignedGetURL(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 objectKey가 주어진 경우")
        class Context_with_non_existing_object_key {

            @Test
            @DisplayName("ExpectedException(404)을 던진다")
            void it_throws_not_found() {
                String objectKey = "ocr-uploads/1/missing.pdf";
                S3Resource resource = mock(S3Resource.class);
                given(resource.exists()).willReturn(false);
                given(s3Template.createResource("hello-test-bucket", objectKey)).willReturn(resource);

                assertThatThrownBy(() -> service.execute(1L, objectKey)).isInstanceOf(ExpectedException.class)
                        .extracting(e -> ((ExpectedException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

                verify(s3Template, never()).createSignedGetURL(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("업로드된 파일이 허용 용량(30MB)을 초과한 경우")
        class Context_with_oversized_object {

            @Test
            @DisplayName("파일을 삭제하고 ExpectedException(413)을 던진다")
            void it_deletes_object_and_throws_payload_too_large() {
                String objectKey = "ocr-uploads/1/huge.pdf";
                S3Resource resource = mock(S3Resource.class);
                given(resource.exists()).willReturn(true);
                given(resource.contentLength()).willReturn(31L * 1024 * 1024);
                given(s3Template.createResource("hello-test-bucket", objectKey)).willReturn(resource);

                assertThatThrownBy(() -> service.execute(1L, objectKey)).isInstanceOf(ExpectedException.class)
                        .extracting(e -> ((ExpectedException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONTENT_TOO_LARGE);

                verify(s3Template).deleteObject("hello-test-bucket", objectKey);
                verify(s3Template, never()).createSignedGetURL(any(), any(), any());
            }
        }
    }
}
