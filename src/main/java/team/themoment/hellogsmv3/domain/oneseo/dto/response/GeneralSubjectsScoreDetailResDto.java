package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneralSubjectsScoreDetailResDto(
        @Schema(description = "1학년 2학기 일반교과 환산 점수. null이면 생략", nullable = true) BigDecimal score1_2,
        @Schema(description = "2학년 1학기 일반교과 환산 점수. null이면 생략", nullable = true) BigDecimal score2_1,
        @Schema(description = "2학년 2학기 일반교과 환산 점수. null이면 생략", nullable = true) BigDecimal score2_2,
        @Schema(description = "3학년 1학기 일반교과 환산 점수. null이면 생략", nullable = true) BigDecimal score3_1,
        @Schema(description = "3학년 2학기 일반교과 환산 점수. null이면 생략", nullable = true) BigDecimal score3_2) {
}
