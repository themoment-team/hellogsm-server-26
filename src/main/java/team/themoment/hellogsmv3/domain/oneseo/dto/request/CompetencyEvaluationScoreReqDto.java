package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CompetencyEvaluationScoreReqDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "역량검사 점수", minimum = "0", maximum = "100", example = "85.5") @NotNull @DecimalMin(value = "0.0", message = "0점 이상이여야 합니다.") @DecimalMax(value = "100.0", message = "100점 이하여야 합니다.") BigDecimal competencyEvaluationScore) {
}
