package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;

@Service
@RequiredArgsConstructor
public class ModifyPersonalInfoService {

    private final OneseoService oneseoService;

    @Transactional
    @CacheEvict(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public void execute(ModifyPersonalInfoReqDto reqDto, Long memberId, boolean checkFirstTest) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        if (checkFirstTest) {
            OneseoService.isBeforeFirstTest(oneseo.getEntranceTestResult().getFirstTestPassYn());
        }

        Member member = oneseo.getMember();
        member.modifyMember(reqDto.name(), reqDto.birth(), member.getPhoneNumber(), reqDto.sex());
    }
}
