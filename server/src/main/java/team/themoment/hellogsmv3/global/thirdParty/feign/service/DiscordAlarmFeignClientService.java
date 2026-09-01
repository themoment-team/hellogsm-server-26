package team.themoment.hellogsmv3.global.thirdParty.feign.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.discord.DiscordAlarmClient;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.DiscordAlarmReqDto;

@Component
@RequiredArgsConstructor
public class DiscordAlarmFeignClientService {

    private final DiscordAlarmClient discordAlarmClient;

    @Value("${discord-alarm.api-key}")
    private String apiKey;

    public void send(DiscordAlarmReqDto reqDto) {
        discordAlarmClient.sendAlarm(reqDto, apiKey);
    }
}
