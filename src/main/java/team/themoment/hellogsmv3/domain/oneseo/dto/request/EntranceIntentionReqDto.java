package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public record EntranceIntentionReqDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "입학등록 동의서 제출 여부", allowableValues = {
                "YES", "NO"}, example = "YES") @NotNull YesNo entranceIntentionYn){
}
