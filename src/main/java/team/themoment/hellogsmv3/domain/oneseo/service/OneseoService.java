package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.operation.repository.OperationTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.FoundMemberAndOneseoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

@Service
@RequiredArgsConstructor
public class OneseoService {

    public static final String ONESEO_CACHE_VALUE = "oneseo";
    private final ScheduleEnvironment scheduleEnv;
    private final OneseoRepository oneseoRepository;
    private final OperationTestResultRepository operationTestResultRepository;
    private final EntranceTestResultRepository entranceTestResultRepository;

    public void assignSubmitCode(Oneseo oneseo, Screening originalScreening) {
        if (oneseo.getWantedScreening() != originalScreening) {
            ScreeningCategory screeningCategory = oneseo.getWantedScreening().getScreeningCategory();
            Integer maxSubmitCodeNumber = oneseoRepository.findMaxSubmitCodeByScreening(screeningCategory);
            int newSubmitCodeNumber = (maxSubmitCodeNumber != null ? maxSubmitCodeNumber : 0) + 1;

            String submitCode;
            switch (screeningCategory) {
                case GENERAL -> submitCode = "A-" + newSubmitCodeNumber;
                case SPECIAL -> submitCode = "B-" + newSubmitCodeNumber;
                case EXTRA -> submitCode = "C-" + newSubmitCodeNumber;
                default -> throw new IllegalArgumentException("Unexpected value: " + screeningCategory);
            }

            oneseo.setOneseoSubmitCode(submitCode);
        }
    }

    public static MiddleSchoolAchievementCalcDto buildCalcDtoWithFillEmpty(MiddleSchoolAchievementReqDto dto,
            GraduationType graduationType) {
        MiddleSchoolAchievementCalcDto.MiddleSchoolAchievementCalcDtoBuilder builder = MiddleSchoolAchievementCalcDto
                .builder();
        if (graduationType == GraduationType.GED) {
            builder.gedAvgScore(dto.gedAvgScore());
            return builder.build();
        }
        // 졸업예정자 & 졸업자는 없는 성적을 복사하여 사용
        // 자유학년제(1학년)을 제외하고,두개 이상의 빈학기가 없다는 가정
        List<Integer> tmpAchievement1_1 = dto.achievement1_1();
        List<Integer> tmpAchievement1_2 = dto.achievement1_2();
        List<Integer> tmpAchievement2_1 = dto.achievement2_1();
        List<Integer> tmpAchievement2_2 = dto.achievement2_2();
        List<Integer> tmpAchievement3_1 = dto.achievement3_1();
        List<Integer> tmpAchievement3_2 = dto.achievement3_2();

        if (graduationType == GraduationType.GRADUATE && tmpAchievement3_2 == null) {
            tmpAchievement3_2 = tmpAchievement3_1;
        } else if (tmpAchievement3_1 == null) {
            tmpAchievement3_1 = tmpAchievement3_2;
        } else if (tmpAchievement2_1 == null) {
            tmpAchievement2_1 = tmpAchievement2_2;
        } else if (tmpAchievement2_2 == null) {
            tmpAchievement2_2 = tmpAchievement2_1;
        } else if (graduationType == GraduationType.CANDIDATE && dto.achievement1_2() == null) {
            if (dto.achievement1_1() == null) {
                tmpAchievement1_2 = tmpAchievement2_2;
            } else {
                tmpAchievement1_2 = tmpAchievement1_1;
            }
        }

        builder.achievement1_2(validationGeneralAchievement(tmpAchievement1_2))
                .achievement2_1(validationGeneralAchievement(tmpAchievement2_1))
                .achievement2_2(validationGeneralAchievement(tmpAchievement2_2))
                .achievement3_1(validationGeneralAchievement(tmpAchievement3_1))
                .achievement3_2(validationGeneralAchievement(tmpAchievement3_2))
                .artsPhysicalAchievement(validationArtsPhysicalAchievement(dto.artsPhysicalAchievement()))
                .absentDays(dto.absentDays()).attendanceDays(dto.attendanceDays()).volunteerTime(dto.volunteerTime())
                .liberalSystem(dto.liberalSystem()).freeSemester(dto.freeSemester()).gedAvgScore(dto.gedAvgScore());
        return builder.build();
    }

    private static List<Integer> validationArtsPhysicalAchievement(List<Integer> achievements) {
        if (achievements == null)
            return null;

        achievements.forEach(achievement -> {
            if (achievement != 0 && (achievement > 5 || achievement < 3))
                throw new ExpectedException("올바르지 않은 예체능 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

    private static List<Integer> validationGeneralAchievement(List<Integer> achievements) {
        if (achievements == null)
            return null;

        achievements.forEach(achievement -> {
            if (achievement > 5 || achievement < 0)
                throw new ExpectedException("올바르지 않은 일반교과 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

    public static Integer calcAbsentDaysCount(List<Integer> absentDays, List<Integer> attendanceDays) {
        if (absentDays == null || attendanceDays == null) {
            return null;
        }

        if (absentDays.stream().anyMatch(Objects::isNull) || attendanceDays.stream().anyMatch(Objects::isNull)) {
            throw new ExpectedException("결석 횟수나 지각, 조퇴, 결과 횟수에 null 값이 포함되어 있습니다.", HttpStatus.BAD_REQUEST);
        }

        int totalAbsentDays = absentDays.stream().mapToInt(Integer::intValue).sum();
        int totalAttendanceDays = attendanceDays.stream().mapToInt(Integer::intValue).sum();

        return totalAbsentDays + (totalAttendanceDays / 3);
    }

    public static void isBeforeFirstTest(YesNo yn) {
        if (yn != null) {
            throw new ExpectedException("1차 전형 결과 산출 이후에는 작업을 진행할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    public static void isBeforeSecondTest(YesNo yn) {
        if (yn != null) {
            throw new ExpectedException("2차 전형 결과 산출 이후에는 작업을 진행할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    public boolean validateFirstTestResultAnnouncement() {
        return operationTestResultRepository.findTestResult()
                .map(testResult -> testResult.getFirstTestResultAnnouncementYn().equals(NO)
                        || LocalDateTime.now().isBefore(scheduleEnv.firstResultsAnnouncement())
                        || entranceTestResultRepository.existsByFirstTestPassYnIsNull())
                .orElse(true);
    }

    public boolean validateSecondTestResultAnnouncement() {
        return operationTestResultRepository.findTestResult()
                .map(testResult -> testResult.getSecondTestResultAnnouncementYn().equals(NO)
                        || LocalDateTime.now().isBefore(scheduleEnv.firstResultsAnnouncement())
                        || entranceTestResultRepository.existsByFirstTestPassYnIsNull())
                .orElse(true);
    }

    public static void isValidMiddleSchoolInfo(OneseoReqDto reqDto) {
        if (reqDto.graduationType().equals(CANDIDATE)
                && (isBlankString(reqDto.schoolTeacherName()) || isBlankString(reqDto.schoolTeacherPhoneNumber())
                        || isBlankString(reqDto.schoolName()) || isBlankString(reqDto.schoolAddress()))) {
            throw new ExpectedException("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isBlankString(String target) {
        return target == null || target.isBlank();
    }

    public Oneseo findWithMemberByMemberIdOrThrow(Long memberId) {
        FoundMemberAndOneseoDto queryResult = oneseoRepository.findMemberAndOneseoByMemberId(memberId);
        if (queryResult.member() == null)
            throw new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND);
        if (queryResult.oneseo() == null)
            throw new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, HttpStatus.NOT_FOUND);
        return queryResult.oneseo();
    }
}
