package team.themoment.hellogsmv3.domain.oneseo.service;

import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@Service
@RequiredArgsConstructor
public class ModifyPersonalInfoService {

    private final MemberService memberService;
    private final OneseoRepository oneseoRepository;
    private final CacheManager cacheManager;

    @Transactional
    public void execute(ModifyPersonalInfoReqDto reqDto, Long memberId, boolean checkFirstTest) {
        Member member = memberService.findByIdOrThrow(memberId);
        Optional<Oneseo> oneseo = oneseoRepository.findByMember(member);

        if (checkFirstTest) {
            oneseo.map(Oneseo::getEntranceTestResult)
                    .ifPresent(result -> OneseoService.isBeforeFirstTest(result.getFirstTestPassYn()));
        }

        member.modifyMember(reqDto.name(), reqDto.birth(), member.getPhoneNumber(), reqDto.sex());

        // 제출된 원서가 DB에 있을 때만 캐시를 무효화한다. 임시저장만 있는 지원자는 DB에 원서가
        // 없으므로 여기서 캐시를 지우면 임시저장 내용 자체가 사라진다(다음 자동 임시저장 때 갱신됨).
        if (oneseo.isPresent()) {
            Cache cache = cacheManager.getCache(OneseoService.ONESEO_CACHE_VALUE);
            if (cache != null) {
                cache.evict(memberId);
            }
        }
    }
}
