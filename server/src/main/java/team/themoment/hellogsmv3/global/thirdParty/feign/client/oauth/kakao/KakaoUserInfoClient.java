package team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.kakao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.KakaoUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.FeignConfig;

@FeignClient(name = "kakao-userinfo-client", url = "https://kapi.kakao.com", configuration = FeignConfig.class)
public interface KakaoUserInfoClient {

    @GetMapping("/v2/user/me")
    KakaoUserInfoResDto getUserInfo(@RequestHeader("Authorization") String authorization);
}
