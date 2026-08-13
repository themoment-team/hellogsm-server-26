package team.themoment.hellogsmv3.domain.oneseo.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractionMetaResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.AchievementTextExtractor;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser;
import team.themoment.sdk.exception.ExpectedException;

/**
 * 업로드된 생활기록부에서 중학교 성적을 추출합니다.
 *
 * <p>
 * 이 서비스는 파일을 저장하지 않고 메모리에서 처리한 뒤 즉시 폐기하며, 원서 데이터를 수정하지 않습니다. 추출 결과는 사용자가 검토하고
 * 수정한 뒤 기존 원서 등록/수정 API로 제출됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractMiddleSchoolAchievementService {

    /** 이 글자 수 미만이면 추출에 실패한 것으로 보고 다음 방식으로 넘어갑니다. */
    private static final int MINIMUM_TEXT_LENGTH = 200;

    private final List<AchievementTextExtractor> textExtractors;
    private final MiddleSchoolRecordParser parser;

    /** 데모 진단용. 추출 원문을 응답에 포함할지 여부이며, 개인정보가 포함되므로 운영 환경에서는 반드시 false여야 합니다. */
    @Value("${oneseo.extraction.expose-raw-text:false}")
    private boolean exposeRawText;

    public ExtractedAchievementResDto execute(MultipartFile file,
            GraduationType graduationType,
            String liberalSystem,
            Long memberId) {
        validateGraduationType(graduationType);
        validateLiberalSystem(graduationType, liberalSystem);
        validateFile(file);

        ExtractedTextDto extractedText = extractWithFallback(readBytes(file), file.getContentType(), memberId);

        ExtractedAchievementResDto result = parser.parse(extractedText, graduationType, liberalSystem);
        log.info("생활기록부 성적 추출 완료. memberId={}, source={}, confidence={}, warningCount={}",
                memberId,
                extractedText.source(),
                result.meta().confidence(),
                result.meta().warnings().size());

        return exposeRawText ? withRawText(result, extractedText.rawText()) : result;
    }

    private void validateGraduationType(GraduationType graduationType) {
        if (graduationType == GraduationType.GED) {
            throw new ExpectedException("검정고시 지원자는 성적 추출을 사용할 수 없습니다. 평균 점수를 직접 입력해주세요.", HttpStatus.BAD_REQUEST);
        }
    }

    /** 예체능 성취점수 배열의 길이가 자유학기제 여부에 따라 달라지므로, 졸업예정자에게는 필수 입력입니다. */
    private void validateLiberalSystem(GraduationType graduationType, String liberalSystem) {
        if (graduationType != GraduationType.CANDIDATE) {
            return;
        }
        if (!MiddleSchoolRecordParser.FREE_YEAR_SYSTEM.equals(liberalSystem)
                && !MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM.equals(liberalSystem)) {
            throw new ExpectedException("졸업예정자는 자유학기제 또는 자유학년제를 지정해야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExpectedException("업로드된 파일이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 등록된 추출기를 순서대로 시도하고, 충분한 텍스트를 얻은 첫 결과를 반환합니다.
     *
     * <p>
     * 텍스트 레이어 방식이 먼저 시도되고 실패하면 OCR로 넘어갑니다. 정부24 발급 생활기록부는 이미지로만 이루어져 있어 실제로는 OCR
     * 경로를 타지만, 텍스트 레이어가 있는 파일이 올라오면 훨씬 빠르고 정확한 경로로 처리됩니다.
     */
    private ExtractedTextDto extractWithFallback(byte[] content, String contentType, Long memberId) {
        List<AchievementTextExtractor> candidates = textExtractors.stream()
                .filter(extractor -> extractor.supports(contentType)).toList();

        if (candidates.isEmpty()) {
            throw new ExpectedException("지원하지 않는 파일 형식입니다. PDF 파일을 업로드해주세요.", HttpStatus.BAD_REQUEST);
        }

        for (AchievementTextExtractor extractor : candidates) {
            ExtractedTextDto extracted = extractor.extract(content, contentType);
            if (hasEnoughText(extracted)) {
                return extracted;
            }
            log.warn("추출 결과가 충분하지 않아 다음 방식으로 넘어갑니다. memberId={}, source={}, 글자수={}",
                    memberId,
                    extracted.source(),
                    extracted.rawText().strip().length());
        }

        throw new ExpectedException("파일에서 성적을 읽지 못했습니다. 다른 파일로 시도하거나 성적을 직접 입력해주세요.", HttpStatus.BAD_REQUEST);
    }

    private boolean hasEnoughText(ExtractedTextDto extracted) {
        return extracted.rawText().strip().length() >= MINIMUM_TEXT_LENGTH;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ExpectedException("파일을 읽는 데 실패했습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private ExtractedAchievementResDto withRawText(ExtractedAchievementResDto result, String rawText) {
        ExtractionMetaResDto meta = result.meta();
        return ExtractedAchievementResDto.builder().achievement(result.achievement())
                .meta(ExtractionMetaResDto.builder().confidence(meta.confidence()).hasTextLayer(meta.hasTextLayer())
                        .pageCount(meta.pageCount()).unrecognizedSubjects(meta.unrecognizedSubjects())
                        .missingSemesters(meta.missingSemesters()).warnings(meta.warnings()).rawText(rawText).build())
                .build();
    }
}
