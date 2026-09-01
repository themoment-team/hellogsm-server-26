package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ExtractMiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractionMetaResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser;
import team.themoment.sdk.exception.ExpectedException;

/**
 * 클라이언트가 보낸 생활기록부 텍스트에서 중학교 성적을 구조화합니다.
 *
 * <p>
 * 파일에서 텍스트를 얻는 일(텍스트 레이어 읽기 또는 OCR)은 클라이언트가 담당합니다. 서버는 원본 파일을 받지도 저장하지도 않으며, 원서
 * 데이터를 수정하지 않습니다. 추출 결과는 사용자가 검토하고 수정한 뒤 기존 원서 등록/수정 API로 제출됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractMiddleSchoolAchievementService {

    /** 이 글자 수 미만이면 성적 표가 담기지 않은 것으로 보고 거절합니다. */
    private static final int MINIMUM_TEXT_LENGTH = 200;

    /**
     * 클라이언트가 인식 신뢰도를 보내지 않았을 때 OCR 경로에 적용할 값. 실제 측정 구간(0.77 ~ 0.83)의 보수적인 하한입니다.
     */
    private static final double DEFAULT_OCR_CONFIDENCE = 0.8;

    private final MiddleSchoolRecordParser parser;

    /** 데모 진단용. 전달받은 원문을 응답에 되돌려줄지 여부이며, 개인정보가 포함되므로 운영 환경에서는 반드시 false여야 합니다. */
    @Value("${oneseo.extraction.expose-raw-text:false}")
    private boolean exposeRawText;

    public ExtractedAchievementResDto execute(ExtractMiddleSchoolAchievementReqDto reqDto, Long memberId) {
        validateGraduationType(reqDto.graduationType());
        validateLiberalSystem(reqDto.graduationType(), reqDto.liberalSystem());
        validateText(reqDto.rawText());

        ExtractedTextDto extractedText = toExtractedText(reqDto);

        ExtractedAchievementResDto result = parser
                .parse(extractedText, reqDto.graduationType(), reqDto.liberalSystem());
        log.info("생활기록부 성적 추출 완료. memberId={}, source={}, hasTextLayer={}, confidence={}, warningCount={}",
                memberId,
                extractedText.source(),
                extractedText.hasTextLayer(),
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

    private void validateText(String rawText) {
        if (rawText.strip().length() < MINIMUM_TEXT_LENGTH) {
            throw new ExpectedException("전달된 텍스트가 너무 짧아 성적을 읽지 못했습니다. 다른 파일로 시도하거나 성적을 직접 입력해주세요.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private ExtractedTextDto toExtractedText(ExtractMiddleSchoolAchievementReqDto reqDto) {
        return new ExtractedTextDto(reqDto.rawText(),
                reqDto.pageCount(),
                reqDto.hasTextLayer(),
                reqDto.source(),
                resolveRecognitionConfidence(reqDto));
    }

    /** 텍스트 레이어에서 그대로 읽었다면 추측이 없으므로 1.0입니다. OCR은 클라이언트가 보고한 값을 씁니다. */
    private double resolveRecognitionConfidence(ExtractMiddleSchoolAchievementReqDto reqDto) {
        if (reqDto.recognitionConfidence() != null) {
            return reqDto.recognitionConfidence();
        }
        return reqDto.source() == ExtractionSource.TEXT_LAYER ? 1.0 : DEFAULT_OCR_CONFIDENCE;
    }

    private ExtractedAchievementResDto withRawText(ExtractedAchievementResDto result, String rawText) {
        ExtractionMetaResDto meta = result.meta();
        return ExtractedAchievementResDto.builder().achievement(result.achievement())
                .meta(ExtractionMetaResDto.builder().confidence(meta.confidence()).hasTextLayer(meta.hasTextLayer())
                        .source(meta.source()).pageCount(meta.pageCount())
                        .unrecognizedSubjects(meta.unrecognizedSubjects()).missingSemesters(meta.missingSemesters())
                        .warnings(meta.warnings()).rawText(rawText).build())
                .build();
    }
}
