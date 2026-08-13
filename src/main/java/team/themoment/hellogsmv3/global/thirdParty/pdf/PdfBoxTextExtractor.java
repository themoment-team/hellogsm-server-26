package team.themoment.hellogsmv3.global.thirdParty.pdf;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.AchievementTextExtractor;
import team.themoment.sdk.exception.ExpectedException;

/**
 * PDF의 텍스트 레이어를 직접 읽어 텍스트를 추출합니다.
 *
 * <p>
 * 텍스트 레이어가 있는 PDF라면 글자를 추측하지 않으므로 가장 정확하고 비용도 들지 않습니다. 다만 정부24에서 발급한 생활기록부는
 * 페이지가 이미지로 들어있어 이 방식이 통하지 않는 것으로 확인되었습니다 (17페이지 문서에서 34자 추출). 이 경우
 * {@code hasTextLayer=false}로 보고되고 서비스가 OCR 구현체로 넘깁니다.
 *
 * <p>
 * 학교에서 자체 발급하거나 다른 경로로 만들어진 PDF에는 텍스트 레이어가 있을 수 있으므로 이 구현체를 먼저 시도합니다.
 */
@Order(1)
@Component
public class PdfBoxTextExtractor implements AchievementTextExtractor {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    /** 이 글자 수 미만이면 텍스트 레이어가 없는 것으로 간주합니다. 생활기록부는 정상 추출 시 수천 자가 나옵니다. */
    private static final int TEXT_LAYER_THRESHOLD = 200;

    @Override
    public boolean supports(String contentType) {
        return PDF_CONTENT_TYPE.equalsIgnoreCase(contentType);
    }

    @Override
    public ExtractedTextDto extract(byte[] content, String contentType) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new ExpectedException("암호가 설정된 PDF는 처리할 수 없습니다. 암호를 해제한 뒤 다시 업로드해주세요.", HttpStatus.BAD_REQUEST);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(document);

            String normalized = rawText == null ? "" : rawText.replace(' ', ' ');
            boolean hasTextLayer = normalized.strip().length() >= TEXT_LAYER_THRESHOLD;

            // 텍스트 레이어는 글자를 추측하지 않고 그대로 읽으므로 인식 신뢰도는 최대입니다.
            return new ExtractedTextDto(normalized,
                    document.getNumberOfPages(),
                    hasTextLayer,
                    ExtractionSource.TEXT_LAYER,
                    1.0);
        } catch (IOException e) {
            throw new ExpectedException("PDF 파일을 읽는 데 실패했습니다. 손상되지 않은 파일인지 확인해주세요.", HttpStatus.BAD_REQUEST);
        }
    }
}
