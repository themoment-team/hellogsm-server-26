package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.*;

import java.util.List;

import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoTempReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.DesiredMajorsResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.FoundOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.MiddleSchoolAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OneseoPrivacyDetailResDto;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda.LambdaScoreCalculatorClient;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class OneseoTempStorageService {

    private final MemberService memberService;
    private final OneseoRepository oneseoRepository;
    private final LambdaScoreCalculatorClient lambdaScoreCalculatorClient;

    @CachePut(value = ONESEO_CACHE_VALUE, key = "#memberId")
    @Transactional(readOnly = true)
    public FoundOneseoResDto execute(OneseoTempReqDto reqDto, Integer step, Long memberId) {
        Member member = memberService.findByIdOrThrow(memberId);

        isNotExistOneseo(member);

        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(member, reqDto);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(reqDto);
        CalculatedScoreResDto calculatedScoreResDto = calculateScore(reqDto);

        return buildFoundOneseoResDto(reqDto,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                step,
                calculatedScoreResDto);
    }

    private void isNotExistOneseo(Member member) {
        if (oneseoRepository.existsByMember(member))
            throw new ExpectedException("이미 원서 제출을 하였습니다.", HttpStatus.BAD_REQUEST);
    }

    private OneseoPrivacyDetailResDto buildOneseoPrivacyDetailResDto(Member member, OneseoTempReqDto reqDto) {

        return OneseoPrivacyDetailResDto.builder().name(member.getName()).sex(member.getSex()).birth(member.getBirth())
                .phoneNumber(member.getPhoneNumber()).graduationType(reqDto.graduationType())
                .graduationDate(reqDto.graduationDate()).address(reqDto.address()).detailAddress(reqDto.detailAddress())
                .guardianName(reqDto.guardianName()).guardianPhoneNumber(reqDto.guardianPhoneNumber())
                .relationshipWithGuardian(reqDto.relationshipWithGuardian()).schoolName(reqDto.schoolName())
                .schoolAddress(reqDto.schoolAddress()).schoolTeacherName(reqDto.schoolTeacherName())
                .schoolTeacherPhoneNumber(reqDto.schoolTeacherPhoneNumber()).profileImg(reqDto.profileImg())
                .studentNumber(reqDto.studentNumber()).build();
    }

    private MiddleSchoolAchievementResDto buildMiddleSchoolAchievementResDto(OneseoTempReqDto reqDto) {
        MiddleSchoolAchievementReqDto middleSchoolAchievement = reqDto.middleSchoolAchievement();

        List<Integer> absentDays = middleSchoolAchievement.absentDays();
        List<Integer> attendanceDays = middleSchoolAchievement.attendanceDays();

        return MiddleSchoolAchievementResDto.builder().achievement1_2(middleSchoolAchievement.achievement1_2())
                .achievement2_1(middleSchoolAchievement.achievement2_1())
                .achievement2_2(middleSchoolAchievement.achievement2_2())
                .achievement3_1(middleSchoolAchievement.achievement3_1())
                .achievement3_2(middleSchoolAchievement.achievement3_2())
                .generalSubjects(middleSchoolAchievement.generalSubjects())
                .newSubjects(middleSchoolAchievement.newSubjects())
                .artsPhysicalAchievement(middleSchoolAchievement.artsPhysicalAchievement())
                .artsPhysicalSubjects(middleSchoolAchievement.artsPhysicalSubjects()).absentDays(absentDays)
                .absentDaysCount(OneseoService.calcAbsentDaysCount(absentDays, attendanceDays))
                .attendanceDays(attendanceDays).volunteerTime(middleSchoolAchievement.volunteerTime())
                .liberalSystem(middleSchoolAchievement.liberalSystem())
                .freeSemester(middleSchoolAchievement.freeSemester()).gedAvgScore(middleSchoolAchievement.gedAvgScore())
                .build();
    }

    private CalculatedScoreResDto calculateScore(OneseoTempReqDto reqDto) {
        LambdaScoreCalculatorReqDto lambdaRequest = LambdaScoreCalculatorReqDto.from(reqDto.middleSchoolAchievement(),
                reqDto.graduationType());
        return lambdaScoreCalculatorClient.calculateScore(lambdaRequest);
    }

    private FoundOneseoResDto buildFoundOneseoResDto(OneseoTempReqDto reqDto,
            OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto,
            MiddleSchoolAchievementResDto middleSchoolAchievementResDto,
            Integer step,
            CalculatedScoreResDto calculatedScoreResDto) {

        return FoundOneseoResDto.builder().oneseoId(null).submitCode(null).wantedScreening(reqDto.screening())
                .desiredMajors(DesiredMajorsResDto.builder().firstDesiredMajor(reqDto.firstDesiredMajor())
                        .secondDesiredMajor(reqDto.secondDesiredMajor()).thirdDesiredMajor(reqDto.thirdDesiredMajor())
                        .build())
                .privacyDetail(oneseoPrivacyDetailResDto).middleSchoolAchievement(middleSchoolAchievementResDto)
                .calculatedScore(calculatedScoreResDto).step(step).build();
    }
}
