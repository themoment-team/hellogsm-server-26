package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;
import static team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM;
import static team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser.FREE_YEAR_SYSTEM;

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
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.SubjectNameNormalizer;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;
import team.themoment.sdk.exception.ExpectedException;

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
        }
        if (tmpAchievement3_1 == null) {
            tmpAchievement3_1 = tmpAchievement3_2;
        }
        if (tmpAchievement2_1 == null) {
            tmpAchievement2_1 = tmpAchievement2_2;
        }
        if (tmpAchievement2_2 == null) {
            tmpAchievement2_2 = tmpAchievement2_1;
        }
        if (tmpAchievement1_2 == null) {
            if (tmpAchievement1_1 == null) {
                tmpAchievement1_2 = tmpAchievement2_2;
            } else {
                tmpAchievement1_2 = tmpAchievement1_1;
            }
        }

        builder.achievement1_1(validationGeneralAchievement(tmpAchievement1_1))
                .achievement1_2(validationGeneralAchievement(tmpAchievement1_2))
                .achievement2_1(validationGeneralAchievement(tmpAchievement2_1))
                .achievement2_2(validationGeneralAchievement(tmpAchievement2_2))
                .achievement3_1(validationGeneralAchievement(tmpAchievement3_1))
                .achievement3_2(validationGeneralAchievement(tmpAchievement3_2))
                .artsPhysicalAchievement(validationArtsPhysicalAchievement(dto.artsPhysicalAchievement(),
                        dto.artsPhysicalSubjects(),
                        graduationType,
                        dto.liberalSystem()))
                .absentDays(dto.absentDays()).attendanceDays(dto.attendanceDays()).volunteerTime(dto.volunteerTime())
                .liberalSystem(dto.liberalSystem()).freeSemester(dto.freeSemester()).gedAvgScore(dto.gedAvgScore());
        return builder.build();
    }

    private static List<Integer> validationArtsPhysicalAchievement(List<Integer> achievements,
            List<String> subjects,
            GraduationType graduationType,
            String liberalSystem) {
        if (subjects != null && !subjects.isEmpty() && (achievements == null || achievements.isEmpty()))
            throw new ExpectedException("예체능 성취점수가 비어있습니다.", HttpStatus.BAD_REQUEST);

        if (achievements == null)
            return null;

        validationArtsPhysicalAchievementSize(achievements, graduationType, liberalSystem);

        achievements.forEach(achievement -> {
            if (achievement == null)
                throw new ExpectedException("예체능 성취점수에 null 값이 포함되어 있습니다.", HttpStatus.BAD_REQUEST);
            if (achievement != 0 && (achievement > 5 || achievement < 3))
                throw new ExpectedException("올바르지 않은 예체능 등급이 입력되었습니다.", HttpStatus.BAD_REQUEST);
        });

        return achievements;
    }

    // 점수 계산 Lambda는 배열에 들어온 칸만 평균에 반영하므로, 길이가 다르면 실제 성적과 다른 점수가 나온다
    private static void validationArtsPhysicalAchievementSize(List<Integer> achievements,
            GraduationType graduationType,
            String liberalSystem) {
        // 자유학기/학년제 값이 올바르지 않으면 기대 길이를 정할 수 없으므로 점수 계산 단계의 검증에 맡긴다
        if (!FREE_SEMESTER_SYSTEM.equals(liberalSystem) && !FREE_YEAR_SYSTEM.equals(liberalSystem))
            return;

        int expectedSize = MiddleSchoolRecordParser.resolveSemesters(graduationType, liberalSystem).size()
                * SubjectNameNormalizer.STANDARD_ARTS_PHYSICAL_SUBJECTS.size();
        if (achievements.size() != expectedSize)
            throw new ExpectedException(
                    "예체능 성취점수 개수가 올바르지 않습니다. 기대값: " + expectedSize + ", 입력값: " + achievements.size(),
                    HttpStatus.BAD_REQUEST);
    }

    private static List<Integer> validationGeneralAchievement(List<Integer> achievements) {
        if (achievements == null)
            return null;

        achievements.forEach(achievement -> {
            if (achievement == null)
                throw new ExpectedException("일반교과 성취점수에 null 값이 포함되어 있습니다.", HttpStatus.BAD_REQUEST);
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
