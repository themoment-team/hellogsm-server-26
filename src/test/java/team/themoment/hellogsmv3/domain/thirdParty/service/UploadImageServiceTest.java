package team.themoment.hellogsmv3.domain.thirdParty.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.data.S3Environment;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.dto.response.UploadImageResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.service.UploadImageService;

@DisplayName("UploadImageService 클래스의")
public class UploadImageServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private S3Environment s3Environment;

    @InjectMocks
    private UploadImageService uploadImageService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        s3Template = mock(S3Template.class);
        s3Environment = mock(S3Environment.class);
        uploadImageService = new UploadImageService(s3Template, s3Environment);

        when(s3Environment.bucketName()).thenReturn("bucket-name");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {
        String s3Key = "some-key";

        @Nested
        @DisplayName("파일이 존재하지 않을 경우")
        class Context_with_non_existing_file {
            MultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> uploadImageService.execute(emptyFile));

                assertEquals("파일이 존재하지 않습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("유효하지 않은 확장자를 가진 파일이 주어지면")
        class Context_with_non_valid_extension {
            MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> uploadImageService.execute(file));

                assertEquals("지원하지 않는 파일 확장자 입니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("유효한 파일이 주어지면")
        class Context_with_valid_file {
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
            S3Resource mockS3Resource = mock(S3Resource.class);

            @BeforeEach
            void setUp() throws Exception {
                URL mockUrl = new URL("https://bucket-name.s3.amazonaws.com/" + s3Key);
                when(mockS3Resource.getURL()).thenReturn(mockUrl);

                given(s3Template.upload(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                        .willReturn(mockS3Resource);
            }

            @Test
            @DisplayName("S3에 업로드된 파일의 URL을 반환한다.")
            void it_returns_uploaded_file_url() {
                UploadImageResDto result = uploadImageService.execute(file);

                assertNotNull(result);
                assertTrue(result.url().contains(s3Key));
            }
        }

        @Nested
        @DisplayName("유효하지 않은 파일을 업로드할 경우")
        class Context_with_non_valid_file {
            MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

            @BeforeEach
            void setUp() {
                given(s3Template.upload(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                        .willThrow(new RuntimeException("AWS S3 upload error"));
            }

            @Test
            @DisplayName("RuntimeException을 던진다.")
            void it_throws_runtime_exception() {
                assertThrows(RuntimeException.class, () -> uploadImageService.execute(file));
            }
        }
    }
}
