package team.themoment.hellogsmv3.global.security.auth.service.provider;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.global.security.auth.dto.UserAuthInfo;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoOAuth2Client;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao.KakaoUserInfoClient;
import team.themoment.sdk.exception.ExpectedException;

@Component
@RequiredArgsConstructor
public class KakaoOAuthProvider implements OAuthProvider {

    private static final String PROVIDER_NAME = "kakao";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final KakaoUserInfoClient kakaoUserInfoClient;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public AuthReferrerType getAuthReferrerType() {
        return AuthReferrerType.KAKAO;
    }

    @Override
    public UserAuthInfo authenticate(String authorizationCode, String redirectUri) {
        validateAuthorizationCode(authorizationCode);

        ClientRegistration clientRegistration = getClientRegistration();
        KakaoTokenResDto tokenResponse = exchangeCodeForToken(authorizationCode, clientRegistration, redirectUri);
        KakaoUserInfoResDto userInfo = getUserInfo(tokenResponse.accessToken());

        String providerId = extractUserEmail(userInfo);
        providerId = providerId == null ? userInfo.id().toString() : providerId;
        validateUserEmail(providerId);

        return new UserAuthInfo(providerId, PROVIDER_NAME, getAuthReferrerType());
    }

    private void validateAuthorizationCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ExpectedException("Authorization code가 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private ClientRegistration getClientRegistration() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(PROVIDER_NAME);
        if (clientRegistration == null) {
            throw new RuntimeException("Kakao OAuth 클라이언트 설정을 찾을 수 없습니다.");
        }
        return clientRegistration;
    }

    private KakaoTokenResDto exchangeCodeForToken(String code,
            ClientRegistration clientRegistration,
            String redirectUri) {
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
                    redirectUri);
            return kakaoOAuth2Client.exchangeCodeForToken(params);
        } catch (Exception e) {
            throw new ExpectedException("Kakao OAuth 토큰 교환에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private KakaoUserInfoResDto getUserInfo(String accessToken) {
        try {
            return kakaoUserInfoClient.getUserInfo(TOKEN_PREFIX + accessToken);
        } catch (Exception e) {
            throw new ExpectedException("Kakao 사용자 정보 조회에 실패했습니다: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractUserEmail(KakaoUserInfoResDto userInfo) {
        return userInfo.getEmail();
    }

    private void validateUserEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ExpectedException("Kakao 사용자 이메일 정보를 가져올 수 없습니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}
