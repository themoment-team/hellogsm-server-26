package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CalculatedScoreResDto(
        @Schema(description = "일반교과 환산 점수. 비GED에서 제공하며 null이면 생략", nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal generalSubjectsScore,
        @Schema(description = "학기별 일반교과 환산 점수. 비GED에서 제공하며 null이면 생략", nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) GeneralSubjectsScoreDetailResDto generalSubjectsScoreDetail,
        @Schema(description = "예체능 환산 점수. 비GED에서 제공하며 null이면 생략", nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal artsPhysicalSubjectsScore,
        @Schema(description = "교과 총점. 원서 조회에서는 GED에 제공하며 null이면 생략", nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal totalSubjectsScore,
        @Schema(description = "출결 환산 점수") BigDecimal attendanceScore,
        @Schema(description = "봉사활동 환산 점수") BigDecimal volunteerScore,
        @Schema(description = "서류평가 총점") BigDecimal totalScore) {
}
