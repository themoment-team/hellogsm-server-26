package team.themoment.hellogsmv3.domain.oneseo.event.handler;

import static team.themoment.hellogsmv3.global.thirdParty.feign.client.type.Env.dev;
import static team.themoment.hellogsmv3.global.thirdParty.feign.client.type.Env.prod;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.event.OneseoApplyEvent;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.DiscordAlarmReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.type.Channel;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.type.Env;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.type.NoticeLevel;
import team.themoment.hellogsmv3.global.thirdParty.feign.service.DiscordAlarmFeignClientService;

@Component
@RequiredArgsConstructor
public class OneseoApplyEventHandler {

    private final OneseoRepository oneseoRepository;
    private final DiscordAlarmFeignClientService discordAlarmFeignClientService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Async("discordTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendDiscordAlarm(OneseoApplyEvent oneseoApplyEvent) {

        String title = makeTitle(oneseoApplyEvent);
        String content = makeContent(oneseoApplyEvent);
        Env env = getEnv();

        DiscordAlarmReqDto reqDto = buildReqDto(title, content, env);

        discordAlarmFeignClientService.send(reqDto);
    }

    private DiscordAlarmReqDto buildReqDto(String title, String content, Env env) {
        return DiscordAlarmReqDto.builder().title(title).content(content).noticeLevel(NoticeLevel.info)
                .channel(Channel.info).env(env).build();
    }

    private String makeTitle(OneseoApplyEvent oneseoApplyEvent) {
        String summitCode = oneseoApplyEvent.getSummitCode();

        return String.format("원서가 작성되었습니다! [%s]", summitCode);
    }

    private String makeContent(OneseoApplyEvent oneseoApplyEvent) {
        long count = oneseoRepository.count();

        String name = oneseoApplyEvent.getName().substring(0, 1);
        String masking = "#".repeat(oneseoApplyEvent.getName().substring(1).length());

        String graduationType = switch (oneseoApplyEvent.getGraduationType()) {
            case CANDIDATE -> "졸업예정자";
            case GRADUATE -> "졸업자";
            case GED -> "검정고시 응시자";
        };

        String screening = switch (oneseoApplyEvent.getScreening()) {
            case GENERAL -> "일반전형";
            case SPECIAL -> "특별전형";
            case EXTRA_ADMISSION, EXTRA_VETERANS -> "정원 외 특별전형";
        };

        return String.format("### 이름 \n%s%s \n### 졸업상태 \n%s \n### 전형 \n%s \n### 현재 작성된 원서 수 \n%s개",
                name,
                masking,
                graduationType,
                screening,
                count);
    }

    private Env getEnv() {
        Env env = activeProfile.equals("prod") ? prod : dev;
        return env;
    }
}
