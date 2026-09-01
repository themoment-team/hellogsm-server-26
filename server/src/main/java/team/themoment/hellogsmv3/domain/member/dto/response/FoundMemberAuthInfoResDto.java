package team.themoment.hellogsmv3.domain.member.dto.response;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;

@Builder
public record FoundMemberAuthInfoResDto(Long memberId, String email, AuthReferrerType authReferrerType, Role role) {
}
