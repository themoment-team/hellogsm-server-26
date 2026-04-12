package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.buildCalcDtoWithFillEmpty;
import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.isValidMiddleSchoolInfo;

import java.util.List;

import org.springframework.cache.annotation.CachePut;
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
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.repository.*;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda.LambdaScoreCalculatorClient;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class ModifyOneseoService {

    private final OneseoRepository oneseoRepository;
    private final OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    private final MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    private final ScreeningChangeHistoryRepository screeningChangeHistoryRepository;
    private final EntranceTestResultRepository entranceTestResultRepository;
    private final EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;
    private final OneseoService oneseoService;
    private final MemberService memberService;
    private final LambdaScoreCalculatorClient lambdaScoreCalculatorClient;

    @Transactional
    @CachePut(value = OneseoService.ONESEO_CACHE_VALUE, key = "#memberId")
    public FoundOneseoResDto execute(OneseoReqDto reqDto, Long memberId) {

        isValidMiddleSchoolInfo(reqDto);

        Oneseo currentOneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        EntranceTestResult entranceTestResult = currentOneseo.getEntranceTestResult();
        OneseoService.isBeforeFirstTest(entranceTestResult.getFirstTestPassYn());

        OneseoPrivacyDetail oneseoPrivacyDetail = oneseoPrivacyDetailRepository.findByOneseo(currentOneseo);
        MiddleSchoolAchievement middleSchoolAchievement = middleSchoolAchievementRepository.findByOneseo(currentOneseo);

        Oneseo modifiedOneseo = buildOneseo(reqDto, currentOneseo);
        oneseoService.assignSubmitCode(modifiedOneseo, currentOneseo.getWantedScreening());

        saveOneseoPrivacyDetail(reqDto, oneseoPrivacyDetail, modifiedOneseo);
        saveMiddleSchoolAchievement(reqDto, middleSchoolAchievement, modifiedOneseo);
        saveHistoryIfWantedScreeningChange(reqDto.screening(), currentOneseo.getWantedScreening(), modifiedOneseo);

        oneseoRepository.save(modifiedOneseo);

        CalculatedScoreResDto calculatedScoreResDto = calculateMiddleSchoolAchievement(reqDto.graduationType(),
                reqDto.middleSchoolAchievement(),
                currentOneseo);
        OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = buildOneseoPrivacyDetailResDto(currentOneseo.getMember(),
                oneseoPrivacyDetail);
        MiddleSchoolAchievementResDto middleSchoolAchievementResDto = buildMiddleSchoolAchievementResDto(
                middleSchoolAchievement);

        return buildOneseoResDto(modifiedOneseo,
                oneseoPrivacyDetailResDto,
                middleSchoolAchievementResDto,
                calculatedScoreResDto);
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

    private Oneseo buildOneseo(OneseoReqDto reqDto, Oneseo oneseo) {
        return Oneseo.builder().id(oneseo.getId()).member(oneseo.getMember())
                .desiredMajors(DesiredMajors.builder().firstDesiredMajor(reqDto.firstDesiredMajor())
                        .secondDesiredMajor(reqDto.secondDesiredMajor()).thirdDesiredMajor(reqDto.thirdDesiredMajor())
                        .build())
                .middleSchoolAchievement(oneseo.getMiddleSchoolAchievement())
                .oneseoPrivacyDetail(oneseo.getOneseoPrivacyDetail()).entranceTestResult(oneseo.getEntranceTestResult())
                .wantedScreeningChangeHistory(oneseo.getWantedScreeningChangeHistory())
                .realOneseoArrivedYn(oneseo.getRealOneseoArrivedYn()).wantedScreening(reqDto.screening())
                .passYn(oneseo.getPassYn()).decidedMajor(oneseo.getDecidedMajor())
                .entranceIntentionYn(oneseo.getEntranceIntentionYn()).oneseoSubmitCode(oneseo.getOneseoSubmitCode())
                .build();
    }

    private void saveOneseoPrivacyDetail(OneseoReqDto reqDto, OneseoPrivacyDetail oneseoPrivacyDetail, Oneseo oneseo) {
        OneseoPrivacyDetail modifiedOneseoPrivacyDetail = OneseoPrivacyDetail.builder().id(oneseoPrivacyDetail.getId())
                .oneseo(oneseo).graduationType(reqDto.graduationType()).graduationDate(reqDto.graduationDate())
                .address(reqDto.address()).detailAddress(reqDto.detailAddress()).profileImg(reqDto.profileImg())
                .guardianName(reqDto.guardianName()).guardianPhoneNumber(reqDto.guardianPhoneNumber())
                .relationshipWithGuardian(reqDto.relationshipWithGuardian()).schoolAddress(reqDto.schoolAddress())
                .schoolName(reqDto.schoolName()).schoolTeacherName(reqDto.schoolTeacherName())
                .schoolTeacherPhoneNumber(reqDto.schoolTeacherPhoneNumber()).studentNumber(reqDto.studentNumber())
                .build();

        oneseo.modifyOneseoPrivacyDetail(modifiedOneseoPrivacyDetail);
        oneseoPrivacyDetailRepository.save(modifiedOneseoPrivacyDetail);
    }

    private void saveMiddleSchoolAchievement(OneseoReqDto reqDto,
            MiddleSchoolAchievement middleSchoolAchievement,
            Oneseo oneseo) {
        MiddleSchoolAchievementReqDto updatedMiddleSchoolAchievement = reqDto.middleSchoolAchievement();

        MiddleSchoolAchievementCalcDto calcDto = buildCalcDtoWithFillEmpty(updatedMiddleSchoolAchievement,
                reqDto.graduationType());

        MiddleSchoolAchievement modifiedMiddleSchoolAchievement = MiddleSchoolAchievement.builder()
                .id(middleSchoolAchievement.getId()).oneseo(oneseo)
                .achievement1_2(validationGeneralAchievement(calcDto.achievement1_2()))
                .achievement2_1(validationGeneralAchievement(calcDto.achievement2_1()))
                .achievement2_2(validationGeneralAchievement(calcDto.achievement2_2()))
                .achievement3_1(validationGeneralAchievement(calcDto.achievement3_1()))
                .achievement3_2(validationGeneralAchievement(calcDto.achievement3_2()))
                .generalSubjects(updatedMiddleSchoolAchievement.generalSubjects())
                .newSubjects(updatedMiddleSchoolAchievement.newSubjects())
                .artsPhysicalAchievement(validationArtsPhysicalAchievement(calcDto.artsPhysicalAchievement()))
                .artsPhysicalSubjects(updatedMiddleSchoolAchievement.artsPhysicalSubjects())
                .absentDays(calcDto.absentDays()).attendanceDays(calcDto.attendanceDays())
                .volunteerTime(calcDto.volunteerTime()).liberalSystem(calcDto.liberalSystem())
                .freeSemester(calcDto.freeSemester()).gedAvgScore(calcDto.gedAvgScore()).build();

        oneseo.modifyMiddleSchoolAchievement(modifiedMiddleSchoolAchievement);
        middleSchoolAchievementRepository.save(modifiedMiddleSchoolAchievement);
    }

    private void saveHistoryIfWantedScreeningChange(Screening afterScreening,
            Screening beforeScreening,
            Oneseo oneseo) {
        if (beforeScreening != afterScreening) {
            WantedScreeningChangeHistory screeningChangeHistory = WantedScreeningChangeHistory.builder()
                    .beforeScreening(beforeScreening).afterScreening(afterScreening).oneseo(oneseo).build();

            oneseo.addWantedScreeningChangeHistory(screeningChangeHistory);
            screeningChangeHistoryRepository.save(screeningChangeHistory);
        }
    }

    private List<Integer> validationGeneralAchievement(List<Integer> achievements) {
        if (achievements == null)
            return null;

        achievements.forEach(achievement -> {
            if (achievement > 5 || achievement < 0)
                throw new ExpectedException("올바르지 않은 일반교과 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

    private List<Integer> validationArtsPhysicalAchievement(List<Integer> achievements) {
        if (achievements == null)
            return null;

        achievements.forEach(achievement -> {
            if (achievement != 0 && (achievement > 5 || achievement < 3))
                throw new ExpectedException("올바르지 않은 예체능 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }
}
