package team.themoment.hellogsmv3.global.thirdParty.feign.client.discord;

import static team.themoment.hellogsmv3.global.security.data.HeaderConstant.*;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.DiscordAlarmReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.config.FeignConfig;

@FeignClient(name = "discord-alarm-client", url = "${discord-alarm.url}", configuration = FeignConfig.class)
public interface DiscordAlarmClient {
    @PostMapping("/notice")
    void sendAlarm(@RequestBody DiscordAlarmReqDto reqDto, @RequestHeader(X_HG_API_KEY) String apiKey);
}
