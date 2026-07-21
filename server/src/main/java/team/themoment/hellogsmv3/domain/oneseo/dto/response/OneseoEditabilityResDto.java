package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;

public record OneseoEditabilityResDto(Boolean oneseoEditability, OneseoEditStatus oneseoEditStatus) {
}
