package team.themoment.hellogsmv3.global.thirdParty.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("PDF 텍스트 레이어 추출기 테스트")
class PdfBoxTextExtractorTest {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private PdfBoxTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfBoxTextExtractor();
    }

    /** 텍스트 레이어를 가진 PDF를 메모리에서 생성합니다. 표준 폰트만 사용하므로 본문은 ASCII로 작성합니다. */
    private byte[] createPdfWithText(int lineCount) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 780);
                for (int i = 0; i < lineCount; i++) {
                    contentStream.showText("achievement row " + i + " with enough characters to pass threshold");
                    contentStream.newLineAtOffset(0, -14);
                }
                contentStream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createEmptyPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(out);
            return out.toByteArray();
        }
    }

    @Nested
    @DisplayName("supports 메서드는")
    class Describe_supports {

        @Test
        @DisplayName("PDF만 처리 가능하다고 응답한다")
        void it_supports_pdf_only() {
            assertThat(extractor.supports(PDF_CONTENT_TYPE)).isTrue();
            assertThat(extractor.supports("APPLICATION/PDF")).isTrue();
            assertThat(extractor.supports("image/png")).isFalse();
        }
    }

    @Nested
    @DisplayName("extract 메서드는")
    class Describe_extract {

        @Nested
        @DisplayName("텍스트 레이어가 있는 PDF가 주어진 경우")
        class Context_with_text_layer_pdf {

            @Test
            @DisplayName("본문 텍스트와 페이지 수를 추출한다")
            void it_extracts_text_and_page_count() throws IOException {
                ExtractedTextDto result = extractor.extract(createPdfWithText(20), PDF_CONTENT_TYPE);

                assertThat(result.hasTextLayer()).isTrue();
                assertThat(result.pageCount()).isEqualTo(1);
                assertThat(result.rawText()).contains("achievement row 0");
            }
        }

        @Nested
        @DisplayName("텍스트가 없는 PDF가 주어진 경우")
        class Context_without_text_layer {

            @Test
            @DisplayName("텍스트 레이어가 없다고 보고한다")
            void it_reports_no_text_layer() throws IOException {
                ExtractedTextDto result = extractor.extract(createEmptyPdf(), PDF_CONTENT_TYPE);

                assertThat(result.hasTextLayer()).isFalse();
                assertThat(result.pageCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("PDF가 아닌 바이트가 주어진 경우")
        class Context_with_corrupted_bytes {

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                byte[] corrupted = "not a pdf".getBytes(StandardCharsets.UTF_8);

                assertThrows(ExpectedException.class, () -> extractor.extract(corrupted, PDF_CONTENT_TYPE));
            }
        }
    }
}
