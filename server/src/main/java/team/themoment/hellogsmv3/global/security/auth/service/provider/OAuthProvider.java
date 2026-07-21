package team.themoment.hellogsmv3.global.security.auth.service.provider;

import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.global.security.auth.dto.UserAuthInfo;

public interface OAuthProvider {

    String TOKEN_PREFIX = "Bearer ";

    String getProviderName();

    AuthReferrerType getAuthReferrerType();

    UserAuthInfo authenticate(String authorizationCode, String redirectUri);
}
