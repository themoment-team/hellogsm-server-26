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
import team.themoment.hellogsmv3.domain.oneseo.dto.response.FoundOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OneseoPrivacyDetailResDto;
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

        // 제출된 원서가 DB에 있을 때는 캐시를 무효화한다. 임시저장만 있는 지원자는 DB에 원서가
        // 없으므로 여기서 캐시를 지우면 임시저장 내용 자체가 사라진다(다음 자동 임시저장 때 갱신됨).
        // 대신 캐시 안의 인적사항(name/birth/sex)만 최신 값으로 갈아끼워서, 임시저장을
        // 다시 하지 않아도 수정 내용이 바로 반영되게 한다.
        if (oneseo.isPresent()) {
            Cache cache = cacheManager.getCache(OneseoService.ONESEO_CACHE_VALUE);
            if (cache != null) {
                cache.evict(memberId);
            }
        } else {
            refreshCachedPersonalInfo(memberId, reqDto);
        }
    }

    private void refreshCachedPersonalInfo(Long memberId, ModifyPersonalInfoReqDto reqDto) {
        Cache cache = cacheManager.getCache(OneseoService.ONESEO_CACHE_VALUE);
        if (cache == null) {
            return;
        }

        FoundOneseoResDto cached = cache.get(memberId, FoundOneseoResDto.class);
        if (cached == null) {
            return;
        }

        OneseoPrivacyDetailResDto oldDetail = cached.privacyDetail();
        OneseoPrivacyDetailResDto updatedDetail = OneseoPrivacyDetailResDto.builder().name(reqDto.name())
                .sex(reqDto.sex()).birth(reqDto.birth()).phoneNumber(oldDetail.phoneNumber())
                .graduationType(oldDetail.graduationType()).address(oldDetail.address())
                .detailAddress(oldDetail.detailAddress()).guardianName(oldDetail.guardianName())
                .guardianPhoneNumber(oldDetail.guardianPhoneNumber())
                .relationshipWithGuardian(oldDetail.relationshipWithGuardian()).schoolName(oldDetail.schoolName())
                .schoolAddress(oldDetail.schoolAddress()).schoolTeacherName(oldDetail.schoolTeacherName())
                .schoolTeacherPhoneNumber(oldDetail.schoolTeacherPhoneNumber()).profileImg(oldDetail.profileImg())
                .graduationDate(oldDetail.graduationDate()).studentNumber(oldDetail.studentNumber()).build();

        FoundOneseoResDto updated = FoundOneseoResDto.builder().oneseoId(cached.oneseoId())
                .submitCode(cached.submitCode()).wantedScreening(cached.wantedScreening())
                .desiredMajors(cached.desiredMajors()).privacyDetail(updatedDetail)
                .middleSchoolAchievement(cached.middleSchoolAchievement()).calculatedScore(cached.calculatedScore())
                .step(cached.step()).build();

        cache.put(memberId, updated);
    }
}
