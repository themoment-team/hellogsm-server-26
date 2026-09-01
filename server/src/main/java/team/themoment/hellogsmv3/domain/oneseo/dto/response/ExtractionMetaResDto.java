package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;

/** 추출 결과의 신뢰도와 진단 정보입니다. */
@Builder
public record ExtractionMetaResDto(@Schema(description = "전체 신뢰도 0.00 ~ 1.00", example = "0.91") BigDecimal confidence,
        @Schema(description = "PDF에 텍스트 레이어가 존재하는지 여부. false면 스캔 이미지 PDF입니다.") boolean hasTextLayer,
        @Schema(description = "텍스트를 얻은 경로. TEXT_LAYER 또는 OCR") ExtractionSource source,
        @Schema(description = "PDF 페이지 수", example = "4") int pageCount,
        @Schema(description = "표준 과목 목록에 매칭하지 못한 원문 과목명") List<String> unrecognizedSubjects,
        @Schema(description = "성적을 찾지 못한 학기 목록", example = "[\"3-2\"]") List<String> missingSemesters,
        @Schema(description = "사용자 검수가 필요한 항목 목록") List<ExtractionWarningResDto> warnings,
        @Schema(description = "추출 원문. 데모 진단용이며 oneseo.extraction.expose-raw-text=true 일 때만 채워집니다.", nullable = true) String rawText) {
}
