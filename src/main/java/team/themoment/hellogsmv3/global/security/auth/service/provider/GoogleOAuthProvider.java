package team.themoment.hellogsmv3.global.security.auth.service.provider;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.auth.dto.UserAuthInfo;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google.GoogleOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google.GoogleUserInfoClient;

@Component
@RequiredArgsConstructor
public class GoogleOAuthProvider implements OAuthProvider {

    private static final String PROVIDER_NAME = "google";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final GoogleOAuth2Client googleOAuth2Client;
    private final GoogleUserInfoClient googleUserInfoClient;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public AuthReferrerType getAuthReferrerType() {
        return AuthReferrerType.GOOGLE;
    }

    @Override
    public UserAuthInfo authenticate(String authorizationCode) {
        validateAuthorizationCode(authorizationCode);

        ClientRegistration clientRegistration = getClientRegistration();
        GoogleTokenResDto tokenResponse = exchangeCodeForToken(authorizationCode, clientRegistration);
        GoogleUserInfoResDto userInfo = getUserInfo(tokenResponse.accessToken());

        validateUserEmail(userInfo.email());

        return new UserAuthInfo(userInfo.email(), PROVIDER_NAME, getAuthReferrerType());
    }

    private void validateAuthorizationCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ExpectedException("Authorization code가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private ClientRegistration getClientRegistration() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(PROVIDER_NAME);
        if (clientRegistration == null) {
            throw new RuntimeException("Google OAuth 클라이언트 설정을 찾을 수 없습니다.");
        }
        return clientRegistration;
    }

    private GoogleTokenResDto exchangeCodeForToken(String code, ClientRegistration clientRegistration) {
        try {
            Map<String, String> params = Map.of("grant_type",
                    clientRegistration.getAuthorizationGrantType().getValue(),
                    "client_id",
                    clientRegistration.getClientId(),
                    "client_secret",
                    clientRegistration.getClientSecret(),
                    "code",
                    code,
                    "redirect_uri",
                    clientRegistration.getRedirectUri());
            return googleOAuth2Client.exchangeCodeForToken(params);
        } catch (Exception e) {
            throw new ExpectedException("Google OAuth 토큰 교환에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private GoogleUserInfoResDto getUserInfo(String accessToken) {
        try {
            return googleUserInfoClient.getUserInfo(TOKEN_PREFIX + accessToken);
        } catch (Exception e) {
            throw new ExpectedException("Google 사용자 정보 조회에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateUserEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ExpectedException("Google 사용자 이메일 정보를 가져올 수 없습니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}
