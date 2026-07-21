package team.themoment.hellogsmv3.domain.member.dto.response;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Builder
public record FoundMemberSecondTestResDto(YesNo secondTestPassYn, Major decidedMajor) {
}
