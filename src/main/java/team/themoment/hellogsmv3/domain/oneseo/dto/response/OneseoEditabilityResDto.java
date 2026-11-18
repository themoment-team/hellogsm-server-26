package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;

public record OneseoEditabilityResDto(
        @Schema(description = "전체 지원자 중 1차 결과가 하나라도 산출되면 false인 전역 수정 가능 여부. 개인 상태·접수 기간은 미반영") Boolean oneseoEditability,
        @Schema(description = "본인 원서의 수정 상태 (NONE, REQUESTED, APPROVED). 원서가 없으면 null", nullable = true) OneseoEditStatus oneseoEditStatus) {
}
