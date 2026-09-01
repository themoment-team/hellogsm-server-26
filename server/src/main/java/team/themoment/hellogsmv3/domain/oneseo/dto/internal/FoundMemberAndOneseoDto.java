package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;

public record FoundMemberAndOneseoDto(Member member, Oneseo oneseo) {
}
