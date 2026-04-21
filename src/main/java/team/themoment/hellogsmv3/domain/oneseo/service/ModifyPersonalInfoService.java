package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModifyPersonalInfoService {

    private final MemberService memberService;
    private final OneseoRepository oneseoRepository;

    @Transactional
    @CacheEvict(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public void execute(ModifyPersonalInfoReqDto reqDto, Long memberId, boolean checkFirstTest) {
        Member member = memberService.findByIdOrThrow(memberId);

        if (checkFirstTest) {
            Optional<Oneseo> oneseo = oneseoRepository.findByMember(member);
            oneseo.map(Oneseo::getEntranceTestResult).ifPresent(result -> OneseoService.isBeforeFirstTest(result.getFirstTestPassYn()));
        }

        member.modifyMember(reqDto.name(), reqDto.birth(), member.getPhoneNumber(), reqDto.sex());
    }
}
