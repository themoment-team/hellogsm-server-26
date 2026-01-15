package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.ONESEO_CACHE_VALUE;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.MiddleSchoolAchievementRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;

@Service
@RequiredArgsConstructor
public class QueryOneseoByIdService {

    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    private final MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    private final MemberService memberService;
    private final OneseoService oneseoService;

    @Cacheable(value = ONESEO_CACHE_VALUE, key = "#memberId")
    @Transactional(readOnly = true)
    public FoundOneseoResDto execute(Long memberId) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);
        OneseoPrivacyDetail oneseoPrivacyDetail = oneseoPrivacyDetailRepository.findByOneseo(oneseo);
        MiddleSchoolAchievement middleSchoolAchievement = middleSchoolAchievementRepository.findByOneseo(oneseo);

        CalculatedScoreResDto calculatedScoreResDto = buildCalculatedScoreResDto(oneseo,
                oneseoPrivacyDetail.getGraduationType());

        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(oneseo.getMember(),
                oneseoPrivacyDetail);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(
                middleSchoolAchievement);

        return buildFoundOneseoResDto(oneseo,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                calculatedScoreResDto);
    }

    private CalculatedScoreResDto buildCalculatedScoreResDto(Oneseo oneseo, GraduationType graduationType) {
        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        EntranceTestFactorsDetail entranceTestFactorsDetail = entranceTestResult.getEntranceTestFactorsDetail();

        GeneralSubjectsScoreDetailResDto generalSubjectsScoreDetailResDto = GeneralSubjectsScoreDetailResDto.builder()
                .score1_2(entranceTestFactorsDetail.getScore1_2()).score2_1(entranceTestFactorsDetail.getScore2_1())
                .score2_2(entranceTestFactorsDetail.getScore2_2()).score3_1(entranceTestFactorsDetail.getScore3_1())
                .score3_2(entranceTestFactorsDetail.getScore3_2()).build();

        return switch (graduationType) {
            case CANDIDATE,
                    GRADUATE ->
                CalculatedScoreResDto.builder()
                        .generalSubjectsScore(entranceTestFactorsDetail.getGeneralSubjectsScore())
                        .artsPhysicalSubjectsScore(entranceTestFactorsDetail.getArtsPhysicalSubjectsScore())
                        .attendanceScore(entranceTestFactorsDetail.getAttendanceScore())
                        .volunteerScore(entranceTestFactorsDetail.getVolunteerScore())
                        .totalScore(entranceTestResult.getDocumentEvaluationScore())
                        .generalSubjectsScoreDetail(generalSubjectsScoreDetailResDto).build();
            case GED ->
                CalculatedScoreResDto.builder().totalSubjectsScore(entranceTestFactorsDetail.getTotalSubjectsScore())
                        .attendanceScore(entranceTestFactorsDetail.getAttendanceScore())
                        .volunteerScore(entranceTestFactorsDetail.getVolunteerScore())
                        .totalScore(entranceTestResult.getDocumentEvaluationScore()).build();
        };
    }

    private OneseoPrivacyDetailResDto buildOneseoPrivacyDetailResDto(Member member,
            OneseoPrivacyDetail oneseoPrivacyDetail) {
        return OneseoPrivacyDetailResDto.builder().name(member.getName()).sex(member.getSex()).birth(member.getBirth())
                .phoneNumber(member.getPhoneNumber()).graduationType(oneseoPrivacyDetail.getGraduationType())
                .graduationDate(oneseoPrivacyDetail.getGraduationDate()).address(oneseoPrivacyDetail.getAddress())
                .detailAddress(oneseoPrivacyDetail.getDetailAddress())
                .guardianName(oneseoPrivacyDetail.getGuardianName())
                .guardianPhoneNumber(oneseoPrivacyDetail.getGuardianPhoneNumber())
                .relationshipWithGuardian(oneseoPrivacyDetail.getRelationshipWithGuardian())
                .schoolName(oneseoPrivacyDetail.getSchoolName()).schoolAddress(oneseoPrivacyDetail.getSchoolAddress())
                .schoolTeacherName(oneseoPrivacyDetail.getSchoolTeacherName())
                .schoolTeacherPhoneNumber(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber())
                .profileImg(oneseoPrivacyDetail.getProfileImg()).studentNumber(oneseoPrivacyDetail.getStudentNumber())
                .build();
    }

    private MiddleSchoolAchievementResDto buildMiddleSchoolAchievementResDto(
            MiddleSchoolAchievement middleSchoolAchievement) {

        List<Integer> absentDays = middleSchoolAchievement.getAbsentDays();
        List<Integer> attendanceDays = middleSchoolAchievement.getAttendanceDays();
        Integer absentDaysCount = OneseoService.calcAbsentDaysCount(absentDays, attendanceDays);

        return MiddleSchoolAchievementResDto.builder().achievement1_2(middleSchoolAchievement.getAchievement1_2())
                .achievement2_1(middleSchoolAchievement.getAchievement2_1())
                .achievement2_2(middleSchoolAchievement.getAchievement2_2())
                .achievement3_1(middleSchoolAchievement.getAchievement3_1())
                .achievement3_2(middleSchoolAchievement.getAchievement3_2())
                .generalSubjects(middleSchoolAchievement.getGeneralSubjects())
                .newSubjects(middleSchoolAchievement.getNewSubjects())
                .artsPhysicalAchievement(middleSchoolAchievement.getArtsPhysicalAchievement())
                .artsPhysicalSubjects(middleSchoolAchievement.getArtsPhysicalSubjects()).absentDays(absentDays)
                .absentDaysCount(absentDaysCount).attendanceDays(attendanceDays)
                .volunteerTime(middleSchoolAchievement.getVolunteerTime())
                .liberalSystem(middleSchoolAchievement.getLiberalSystem())
                .freeSemester(middleSchoolAchievement.getFreeSemester())
                .gedAvgScore(middleSchoolAchievement.getGedAvgScore()).build();
    }

    private FoundOneseoResDto buildFoundOneseoResDto(Oneseo oneseo,
            OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto,
            MiddleSchoolAchievementResDto middleSchoolAchievementResDto,
            CalculatedScoreResDto calculatedScoreResDto) {
        DesiredMajors desiredMajors = oneseo.getDesiredMajors();

        return FoundOneseoResDto.builder().oneseoId(oneseo.getId()).submitCode(oneseo.getOneseoSubmitCode())
                .wantedScreening(oneseo.getWantedScreening())
                .desiredMajors(DesiredMajorsResDto.builder().firstDesiredMajor(desiredMajors.getFirstDesiredMajor())
                        .secondDesiredMajor(desiredMajors.getSecondDesiredMajor())
                        .thirdDesiredMajor(desiredMajors.getThirdDesiredMajor()).build())
                .privacyDetail(oneseoPrivacyDetailResDto).middleSchoolAchievement(middleSchoolAchievementResDto)
                .calculatedScore(calculatedScoreResDto).build();
    }
}
