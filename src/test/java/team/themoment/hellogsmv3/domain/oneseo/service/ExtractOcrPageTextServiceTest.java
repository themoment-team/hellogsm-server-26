package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrPageTextResDto;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocAchievementTextConverter;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocConversionResult;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocOcrClient;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.OcrConcurrencyGate;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OCR 페이지 텍스트 추출 서비스 테스트")
class ExtractOcrPageTextServiceTest {

    @Mock
    private KordocOcrClient kordocOcrClient;

    @Mock
    private KordocAchievementTextConverter converter;

    private ExtractOcrPageTextService service;

    @BeforeEach
    void setUp() {
        service = new ExtractOcrPageTextService(kordocOcrClient, converter, new OcrConcurrencyGate(1, 5));
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("파일이 존재하지 않는 경우")
        class Context_with_empty_file {

            @Test
            @DisplayName("ExpectedException을 던지고 OCR을 실행하지 않는다")
            void it_throws_expected_exception() {
                MultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

                ExpectedException exception = assertThrows(ExpectedException.class, () -> service.execute(emptyFile));

                assertThat(exception.getMessage()).isEqualTo("파일이 존재하지 않습니다.");
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                verify(kordocOcrClient, never()).recognize(any());
            }
        }

        @Nested
        @DisplayName("지원하지 않는 확장자가 주어진 경우")
        class Context_with_invalid_extension {

            @Test
            @DisplayName("ExpectedException을 던지고 OCR을 실행하지 않는다")
            void it_throws_expected_exception() {
                MultipartFile file = new MockMultipartFile("file", "page.pdf", "application/pdf", "data".getBytes());

                ExpectedException exception = assertThrows(ExpectedException.class, () -> service.execute(file));

                assertThat(exception.getMessage()).isEqualTo("지원하지 않는 파일 확장자입니다.");
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                verify(kordocOcrClient, never()).recognize(any());
            }
        }

        @Nested
        @DisplayName("유효한 스캔 이미지가 주어진 경우")
        class Context_with_valid_image {

            @Test
            @DisplayName("변환 결과를 반환하고 임시 이미지 파일을 삭제한다")
            void it_returns_conversion_result_and_deletes_temp_file() {
                MultipartFile file = new MockMultipartFile("file", "page.jpg", "image/jpeg", "fake-image".getBytes());
                KordocParseResult parseResult = new KordocParseResult(List.of());
                KordocConversionResult conversionResult = new KordocConversionResult("[1학년]\n국어 91/77.8 A(168)",
                        List.of());

                given(kordocOcrClient.recognize(any())).willReturn(parseResult);
                given(converter.convert(parseResult)).willReturn(conversionResult);

                OcrPageTextResDto result = service.execute(file);

                assertThat(result.rawText()).isEqualTo(conversionResult.rawText());
                assertThat(result.unrecognizedSubjectBlobs()).isEmpty();

                ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
                verify(kordocOcrClient).recognize(pathCaptor.capture());
                assertThat(Files.exists(pathCaptor.getValue())).isFalse();
            }
        }

        @Nested
        @DisplayName("긴 변이 2000px을 넘는 이미지가 주어진 경우")
        class Context_with_oversized_image {

            @Test
            @DisplayName("kordoc에 전달하기 전 긴 변을 2000px로 축소한다")
            void it_downscales_before_passing_to_kordoc() throws IOException {
                byte[] oversizedJpeg = createJpeg(3000, 1500);
                MultipartFile file = new MockMultipartFile("file", "page.jpg", "image/jpeg", oversizedJpeg);
                KordocParseResult parseResult = new KordocParseResult(List.of());
                AtomicInteger capturedLongestSide = new AtomicInteger();

                given(kordocOcrClient.recognize(any())).willAnswer(invocation -> {
                    Path path = invocation.getArgument(0);
                    BufferedImage image = ImageIO.read(path.toFile());
                    capturedLongestSide.set(Math.max(image.getWidth(), image.getHeight()));
                    return parseResult;
                });
                given(converter.convert(parseResult)).willReturn(new KordocConversionResult("", List.of()));

                service.execute(file);

                assertThat(capturedLongestSide.get()).isEqualTo(2000);
            }

            private byte[] createJpeg(int width, int height) throws IOException {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", out);
                return out.toByteArray();
            }
        }
    }
}
