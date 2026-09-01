package team.themoment.hellogsmv3.global.thirdParty.feign.client.oauth.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response.GoogleUserInfoResDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.FeignConfig;

@FeignClient(name = "google-userinfo-client", url = "https://www.googleapis.com", configuration = FeignConfig.class)
public interface GoogleUserInfoClient {

    @GetMapping("/oauth2/v2/userinfo")
    GoogleUserInfoResDto getUserInfo(@RequestHeader("Authorization") String authorization);
}
