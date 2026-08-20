package team.themoment.hellogsmv3.domain.oneseo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrPageTextResDto;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocAchievementTextConverter;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocConversionResult;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocOcrClient;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.OcrConcurrencyGate;
import team.themoment.sdk.exception.ExpectedException;

/**
 * 텍스트 레이어가 없는 생활기록부 페이지의 스캔 이미지를 kordoc으로 인식해, 기존
 * {@link team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser}가
 * 읽는 rawText 줄 형식으로 되돌립니다.
 *
 * <p>
 * 업로드된 이미지는 kordoc 실행을 위해 임시 파일로만 잠깐 존재하고, 처리 직후(성공 · 실패 무관) 삭제되며 서버에 영구 저장되지
 * 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractOcrPageTextService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");

    private final KordocOcrClient kordocOcrClient;
    private final KordocAchievementTextConverter converter;
    private final OcrConcurrencyGate concurrencyGate;

    public OcrPageTextResDto execute(MultipartFile file) {
        validate(file);

        Path tempImage = writeToTempFile(file);
        try {
            KordocParseResult parseResult = concurrencyGate.runWithinLimit(() -> kordocOcrClient.recognize(tempImage));
            KordocConversionResult conversion = converter.convert(parseResult);

            if (!conversion.unrecognizedSubjectBlobs().isEmpty()) {
                log.warn("OCR 페이지에서 과목명 분해 실패. unrecognizedSubjectBlobCount={}",
                        conversion.unrecognizedSubjectBlobs().size());
            }

            return OcrPageTextResDto.builder().rawText(conversion.rawText())
                    .unrecognizedSubjectBlobs(conversion.unrecognizedSubjectBlobs()).build();
        } finally {
            deleteQuietly(tempImage);
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ExpectedException("파일이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ExpectedException("지원하지 않는 파일 확장자입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private Path writeToTempFile(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        try {
            Path tempFile = Files.createTempFile("ocr-page-", "." + extension);
            file.transferTo(tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new ExpectedException("업로드된 이미지를 처리하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("OCR 임시 이미지 삭제 실패. path={}", file);
        }
    }
}
