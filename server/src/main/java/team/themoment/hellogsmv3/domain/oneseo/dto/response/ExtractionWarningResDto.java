package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/** 사용자 검수가 필요한 항목 하나를 나타냅니다. 프론트엔드는 field/index로 해당 입력칸을 강조 표시할 수 있습니다. */
@Builder
public record ExtractionWarningResDto(@Schema(description = "대상 필드명", example = "achievement2_1") String field,
        @Schema(description = "과목 인덱스. null이면 필드 전체를 의미합니다.", nullable = true, example = "2") Integer index,
        @Schema(description = "추출된 원문", nullable = true, example = "사회(역사포함)") String rawText,
        @Schema(description = "검수 요청 사유") ExtractionWarningType type,
        @Schema(description = "사용자에게 노출할 안내 문구", example = "'사회(역사포함)' 과목을 인식하지 못했습니다. 직접 확인해주세요.") String message) {
}
