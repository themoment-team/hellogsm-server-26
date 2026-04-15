package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class ModifyPersonalInfoService {

    private final OneseoService oneseoService;
    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;

    @Transactional
    @CacheEvict(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public void execute(ModifyPersonalInfoReqDto reqDto, Long memberId) {

        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        OneseoService.isBeforeFirstTest(oneseo.getEntranceTestResult().getFirstTestPassYn());

        if (reqDto.graduationType() == GraduationType.CANDIDATE
                && (isBlank(reqDto.schoolTeacherName()) || isBlank(reqDto.schoolTeacherPhoneNumber())
                        || isBlank(reqDto.schoolName()) || isBlank(reqDto.schoolAddress()))) {
            throw new ExpectedException("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }

        Member member = oneseo.getMember();
        member.modifyMember(reqDto.name(), reqDto.birth(), member.getPhoneNumber(), reqDto.sex());

        OneseoPrivacyDetail existing = oneseoPrivacyDetailRepository.findByOneseo(oneseo);
        OneseoPrivacyDetail updated = OneseoPrivacyDetail.builder().id(existing.getId()).oneseo(oneseo)
                .graduationType(reqDto.graduationType()).graduationDate(reqDto.graduationDate())
                .address(reqDto.address()).detailAddress(reqDto.detailAddress()).profileImg(reqDto.profileImg())
                .guardianName(reqDto.guardianName()).guardianPhoneNumber(reqDto.guardianPhoneNumber())
                .relationshipWithGuardian(reqDto.relationshipWithGuardian()).schoolName(reqDto.schoolName())
                .schoolAddress(reqDto.schoolAddress()).schoolTeacherName(reqDto.schoolTeacherName())
                .schoolTeacherPhoneNumber(reqDto.schoolTeacherPhoneNumber()).studentNumber(reqDto.studentNumber())
                .build();

        oneseoPrivacyDetailRepository.save(updated);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
