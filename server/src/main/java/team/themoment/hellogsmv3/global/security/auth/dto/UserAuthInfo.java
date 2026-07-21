package team.themoment.hellogsmv3.global.security.auth.dto;

import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;

public record UserAuthInfo(String email, String provider, AuthReferrerType authReferrerType) {
}
