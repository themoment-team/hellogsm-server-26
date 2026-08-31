package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;
import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.buildCalcDtoWithFillEmpty;
import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.isValidMiddleSchoolInfo;

import java.util.List;

import org.springframework.cache.annotation.CachePut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.event.OneseoApplyEvent;
import team.themoment.hellogsmv3.domain.oneseo.repository.*;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda.LambdaScoreCalculatorClient;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class CreateOneseoService {

    private final OneseoRepository oneseoRepository;
    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    private final MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    private final EntranceTestResultRepository entranceTestResultRepository;
    private final EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;
    private final MemberService memberService;
    private final LambdaScoreCalculatorClient lambdaScoreCalculatorClient;
    private final OneseoService oneseoService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    @CachePut(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public FoundOneseoResDto execute(OneseoReqDto reqDto, Long memberId) {

        isValidMiddleSchoolInfo(reqDto);

        Member currentMember = memberService.findByIdForUpdateOrThrow(memberId);

        isExistOneseo(currentMember);

        Oneseo oneseo = buildOneseo(reqDto, currentMember);
        OneseoPrivacyDetail oneseoPrivacyDetail = buildOneseoPrivacyDetail(reqDto, oneseo);
        MiddleSchoolAchievement middleSchoolAchievement = buildMiddleSchoolAchievement(reqDto, oneseo);

        oneseoService.assignSubmitCode(oneseo, null);

        saveEntities(oneseo, oneseoPrivacyDetail, middleSchoolAchievement);

        CalculatedScoreResDto calculatedScoreResDto = calculateMiddleSchoolAchievement(reqDto.graduationType(),
                reqDto.middleSchoolAchievement(),
                oneseo);

        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(currentMember,
                oneseoPrivacyDetail);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(
                middleSchoolAchievement);

        sendOneseoApplyEvent(currentMember, oneseo, oneseoPrivacyDetail);

        return buildOneseoResDto(oneseo,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                calculatedScoreResDto);
    }

    private void sendOneseoApplyEvent(Member currentMember, Oneseo oneseo, OneseoPrivacyDetail oneseoPrivacyDetail) {
        OneseoApplyEvent oneseoApplyEvent = OneseoApplyEvent.builder().name(currentMember.getName())
                .summitCode(oneseo.getOneseoSubmitCode()).graduationType(oneseoPrivacyDetail.getGraduationType())
                .screening(oneseo.getWantedScreening()).build();

        applicationEventPublisher.publishEvent(oneseoApplyEvent);
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

        return MiddleSchoolAchievementResDto.builder().achievement1_1(middleSchoolAchievement.getAchievement1_1())
                .achievement1_2(middleSchoolAchievement.getAchievement1_2())
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

    private FoundOneseoResDto buildOneseoResDto(Oneseo oneseo,
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

    private CalculatedScoreResDto calculateMiddleSchoolAchievement(GraduationType graduationType,
            MiddleSchoolAchievementReqDto middleSchoolAchievement,
            Oneseo oneseo) {
        LambdaScoreCalculatorReqDto lambdaRequest = LambdaScoreCalculatorReqDto.from(middleSchoolAchievement,
                graduationType);
        CalculatedScoreResDto calculatedScore = lambdaScoreCalculatorClient.calculateScore(lambdaRequest);

        saveCalculatedScoreToDb(calculatedScore, oneseo);

        return calculatedScore;
    }

    private void saveCalculatedScoreToDb(CalculatedScoreResDto calculatedScore, Oneseo oneseo) {
        EntranceTestResult findEntranceTestResult = entranceTestResultRepository.findByOneseo(oneseo);

        if (findEntranceTestResult == null) {
            EntranceTestFactorsDetail entranceTestFactorsDetail = EntranceTestFactorsDetail.builder()
                    .generalSubjectsScore(calculatedScore.generalSubjectsScore())
                    .artsPhysicalSubjectsScore(calculatedScore.artsPhysicalSubjectsScore())
                    .totalSubjectsScore(calculatedScore.totalSubjectsScore() != null
                            ? calculatedScore.totalSubjectsScore()
                            : (calculatedScore.generalSubjectsScore() != null
                                    && calculatedScore.artsPhysicalSubjectsScore() != null
                                            ? calculatedScore.generalSubjectsScore()
                                                    .add(calculatedScore.artsPhysicalSubjectsScore())
                                            : null))
                    .attendanceScore(calculatedScore.attendanceScore()).volunteerScore(calculatedScore.volunteerScore())
                    .totalNonSubjectsScore(
                            calculatedScore.attendanceScore() != null && calculatedScore.volunteerScore() != null
                                    ? calculatedScore.attendanceScore().add(calculatedScore.volunteerScore())
                                    : null)
                    .score1_2(calculatedScore.generalSubjectsScoreDetail() != null
                            ? calculatedScore.generalSubjectsScoreDetail().score1_2()
                            : null)
                    .score2_1(calculatedScore.generalSubjectsScoreDetail() != null
                            ? calculatedScore.generalSubjectsScoreDetail().score2_1()
                            : null)
                    .score2_2(calculatedScore.generalSubjectsScoreDetail() != null
                            ? calculatedScore.generalSubjectsScoreDetail().score2_2()
                            : null)
                    .score3_1(calculatedScore.generalSubjectsScoreDetail() != null
                            ? calculatedScore.generalSubjectsScoreDetail().score3_1()
                            : null)
                    .score3_2(calculatedScore.generalSubjectsScoreDetail() != null
                            ? calculatedScore.generalSubjectsScoreDetail().score3_2()
                            : null)
                    .build();

            EntranceTestResult entranceTestResult = new EntranceTestResult(oneseo,
                    entranceTestFactorsDetail,
                    calculatedScore.totalScore());

            entranceTestFactorsDetailRepository.save(entranceTestFactorsDetail);
            entranceTestResultRepository.save(entranceTestResult);
        } else {
            EntranceTestFactorsDetail findEntranceTestFactorsDetail = findEntranceTestResult
                    .getEntranceTestFactorsDetail();

            if (calculatedScore.generalSubjectsScore() != null) {
                findEntranceTestFactorsDetail.updateGradeEntranceTestFactorsDetail(
                        calculatedScore.generalSubjectsScore(),
                        calculatedScore.artsPhysicalSubjectsScore(),
                        calculatedScore.totalSubjectsScore() != null
                                ? calculatedScore.totalSubjectsScore()
                                : calculatedScore.generalSubjectsScore()
                                        .add(calculatedScore.artsPhysicalSubjectsScore()),
                        calculatedScore.attendanceScore(),
                        calculatedScore.volunteerScore(),
                        calculatedScore.attendanceScore().add(calculatedScore.volunteerScore()),
                        calculatedScore.generalSubjectsScoreDetail() != null
                                ? calculatedScore.generalSubjectsScoreDetail().score1_2()
                                : null,
                        calculatedScore.generalSubjectsScoreDetail() != null
                                ? calculatedScore.generalSubjectsScoreDetail().score2_1()
                                : null,
                        calculatedScore.generalSubjectsScoreDetail() != null
                                ? calculatedScore.generalSubjectsScoreDetail().score2_2()
                                : null,
                        calculatedScore.generalSubjectsScoreDetail() != null
                                ? calculatedScore.generalSubjectsScoreDetail().score3_1()
                                : null,
                        calculatedScore.generalSubjectsScoreDetail() != null
                                ? calculatedScore.generalSubjectsScoreDetail().score3_2()
                                : null);
            } else {
                findEntranceTestFactorsDetail.updateGedEntranceTestFactorsDetail(calculatedScore.attendanceScore(),
                        calculatedScore.volunteerScore(),
                        calculatedScore.totalSubjectsScore(),
                        calculatedScore.attendanceScore().add(calculatedScore.volunteerScore()));
            }

            findEntranceTestResult.modifyDocumentEvaluationScore(calculatedScore.totalScore());

            oneseo.modifyEntranceTestResult(findEntranceTestResult);
            entranceTestFactorsDetailRepository.save(findEntranceTestFactorsDetail);
            entranceTestResultRepository.save(findEntranceTestResult);
        }
    }

    private void saveEntities(Oneseo oneseo,
            OneseoPrivacyDetail oneseoPrivacyDetail,
            MiddleSchoolAchievement middleSchoolAchievement) {
        oneseoRepository.save(oneseo);
        oneseoPrivacyDetailRepository.save(oneseoPrivacyDetail);
        middleSchoolAchievementRepository.save(middleSchoolAchievement);
    }

    private Oneseo buildOneseo(OneseoReqDto reqDto, Member currentMember) {
        return Oneseo.builder().member(currentMember)
                .desiredMajors(DesiredMajors.builder().firstDesiredMajor(reqDto.firstDesiredMajor())
                        .secondDesiredMajor(reqDto.secondDesiredMajor()).thirdDesiredMajor(reqDto.thirdDesiredMajor())
                        .build())
                .realOneseoArrivedYn(NO).wantedScreening(reqDto.screening()).build();
    }

    private OneseoPrivacyDetail buildOneseoPrivacyDetail(OneseoReqDto reqDto, Oneseo oneseo) {
        return OneseoPrivacyDetail.builder().oneseo(oneseo).graduationType(reqDto.graduationType())
                .graduationDate(reqDto.graduationDate()).address(reqDto.address()).detailAddress(reqDto.detailAddress())
                .profileImg(reqDto.profileImg()).guardianName(reqDto.guardianName())
                .guardianPhoneNumber(reqDto.guardianPhoneNumber())
                .relationshipWithGuardian(reqDto.relationshipWithGuardian()).schoolAddress(reqDto.schoolAddress())
                .schoolName(reqDto.schoolName()).schoolTeacherName(reqDto.schoolTeacherName())
                .schoolTeacherPhoneNumber(reqDto.schoolTeacherPhoneNumber()).studentNumber(reqDto.studentNumber())
                .build();
    }

    private MiddleSchoolAchievement buildMiddleSchoolAchievement(OneseoReqDto reqDto, Oneseo oneseo) {
        MiddleSchoolAchievement.MiddleSchoolAchievementBuilder builder = MiddleSchoolAchievement.builder();
        MiddleSchoolAchievementReqDto middleSchoolAchievement = reqDto.middleSchoolAchievement();
        // calcDto를 통해 성적복사 처리
        MiddleSchoolAchievementCalcDto calcDto = buildCalcDtoWithFillEmpty(reqDto.middleSchoolAchievement(),
                reqDto.graduationType());

        builder.oneseo(oneseo).achievement1_1(calcDto.achievement1_1()).achievement1_2(calcDto.achievement1_2())
                .achievement2_1(calcDto.achievement2_1()).achievement2_2(calcDto.achievement2_2())
                .achievement3_1(calcDto.achievement3_1()).achievement3_2(calcDto.achievement3_2())
                .generalSubjects(middleSchoolAchievement.generalSubjects())
                .newSubjects(middleSchoolAchievement.newSubjects())
                .artsPhysicalAchievement(calcDto.artsPhysicalAchievement())
                .artsPhysicalSubjects(middleSchoolAchievement.artsPhysicalSubjects()).absentDays(calcDto.absentDays())
                .attendanceDays(calcDto.attendanceDays()).volunteerTime(calcDto.volunteerTime())
                .liberalSystem(calcDto.liberalSystem()).freeSemester(calcDto.freeSemester())
                .gedAvgScore(calcDto.gedAvgScore());

        return builder.build();
    }

    private void isExistOneseo(Member currentMember) {
        if (oneseoRepository.existsByMember(currentMember)) {
            throw new ExpectedException("이미 원서가 존재합니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
