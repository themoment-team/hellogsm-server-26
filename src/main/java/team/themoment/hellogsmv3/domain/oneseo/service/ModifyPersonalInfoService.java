package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;

@Service
@RequiredArgsConstructor
public class ModifyPersonalInfoService {

    private final MemberService memberService;

    @Transactional
    @CacheEvict(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public void execute(ModifyPersonalInfoReqDto reqDto, Long memberId) {
        Member member = memberService.findByIdForUpdateOrThrow(memberId);
        member.modifyMember(reqDto.name(), reqDto.birth(), member.getPhoneNumber(), reqDto.sex());
    }
}
