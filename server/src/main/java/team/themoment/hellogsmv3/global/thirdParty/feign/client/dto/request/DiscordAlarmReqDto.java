package team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.type.Channel;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.type.Env;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.type.NoticeLevel;

@Getter
@Builder
@AllArgsConstructor
public class DiscordAlarmReqDto {
    private String title;
    private String content;
    private NoticeLevel noticeLevel;
    private Channel channel;
    private Env env;
}
