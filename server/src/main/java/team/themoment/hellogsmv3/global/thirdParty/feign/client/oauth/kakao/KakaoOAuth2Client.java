package team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoTokenResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.FeignConfig;

@FeignClient(name = "kakao-oauth2-client", url = "https://kauth.kakao.com", configuration = FeignConfig.class)
public interface KakaoOAuth2Client {

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KakaoTokenResDto exchangeCodeForToken(@RequestBody Map<String, ?> formParams);
}
