package team.themoment.hellogsmv3.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Builder
public record FoundMemberSecondTestResDto(
        @Schema(description = "2차 전형 합격 여부. 1차 불합격 등 결과 미입력 시 null", example = "YES", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) YesNo secondTestPassYn,
        @Schema(description = "배정 학과. 미배정 시 null", example = "SW", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) Major decidedMajor) {
}
