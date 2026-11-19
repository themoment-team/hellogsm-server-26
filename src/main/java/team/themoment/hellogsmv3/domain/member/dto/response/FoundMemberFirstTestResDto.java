package team.themoment.hellogsmv3.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Builder
public record FoundMemberFirstTestResDto(
        @Schema(description = "1차 전형 합격 여부", example = "YES", requiredMode = Schema.RequiredMode.REQUIRED) YesNo firstTestPassYn) {
}
