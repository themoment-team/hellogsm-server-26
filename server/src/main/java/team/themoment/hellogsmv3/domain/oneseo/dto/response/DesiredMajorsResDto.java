package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;

@Builder
public record DesiredMajorsResDto(Major firstDesiredMajor, Major secondDesiredMajor, Major thirdDesiredMajor) {
}
