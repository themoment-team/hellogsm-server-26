package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.GED;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.FoundMemberAndOneseoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.MiddleSchoolAchievementCalcDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("OneseoService 클래스의")
public class OneseoServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private OneseoService oneseoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("findWithMemberByMemberIdOrThrow 메소드는")
    class Describe_findWithMemberByMemberIdOrThrow {

        private final Long memberId = 1L;
        private final Member member = Member.builder().id(memberId).build();
        private final Oneseo oneseo = Oneseo.builder().member(member).build();

        @Nested
        @DisplayName("존재하는 회원 ID와 원서가 주어지면")
        class Context_with_existing_member_id_and_oneseo {

            @BeforeEach
            void setUp() {
                FoundMemberAndOneseoDto queryResult = new FoundMemberAndOneseoDto(member, oneseo);
                given(oneseoRepository.findMemberAndOneseoByMemberId(memberId)).willReturn(queryResult);
            }

            @Test
            @DisplayName("Oneseo 객체를 반환한다.")
            void it_returns_oneseo() {
                Oneseo foundOneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);
                assertEquals(oneseo, foundOneseo);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                FoundMemberAndOneseoDto queryResult = new FoundMemberAndOneseoDto(null, null);
                given(oneseoRepository.findMemberAndOneseoByMemberId(memberId)).willReturn(queryResult);
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception_for_non_existing_member() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> oneseoService.findWithMemberByMemberIdOrThrow(memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("존재하는 회원 ID이지만 원서가 없으면")
        class Context_with_existing_member_id_but_no_oneseo {

            @BeforeEach
            void setUp() {
                FoundMemberAndOneseoDto queryResult = new FoundMemberAndOneseoDto(member, null);
                given(oneseoRepository.findMemberAndOneseoByMemberId(memberId)).willReturn(queryResult);
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception_for_non_existing_oneseo() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> oneseoService.findWithMemberByMemberIdOrThrow(memberId));

                assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("calcAbsentDaysCount 메소드는")
    class Describe_calcAbsentDaysCount {

        @Nested
        @DisplayName("결석 횟수 list와 지각, 조퇴, 결과 횟수 list를 보내면")
        class Context_with_absent_attendance_days {

            List<Integer> absentDays = List.of(3, 0, 0);
            List<Integer> attendanceDays = List.of(0, 0, 0, 1, 0, 1, 0, 2, 2);

            @Test
            @DisplayName("환산일수를 반환한다.")
            void it_returns_oneseo() {
                Integer absentDaysCount = OneseoService.calcAbsentDaysCount(absentDays, attendanceDays);
                assertEquals(5, absentDaysCount);
            }
        }

        @Nested
        @DisplayName("null 값이 결석 횟수 list와 지각, 조퇴, 결과 횟수 list를 보내면")
        class Context_with_null_absent_attendance_days {

            List<Integer> nullAbsentDays = null;
            List<Integer> nullAttendanceDays = null;

            @Test
            @DisplayName("null 값을 반환한다.")
            void it_returns_oneseo() {
                Integer absentDaysCount = OneseoService.calcAbsentDaysCount(nullAbsentDays, nullAttendanceDays);
                assertNull(absentDaysCount);
            }
        }

        @Nested
        @DisplayName("결석 횟수 list나 지각, 조퇴, 결과 횟수 list에 null 값이 포함되어 있으면")
        class Context_with_null_in_absent_attendance_days_list {

            List<Integer> nullInAbsentDays = Arrays.asList(3, 0, null);
            List<Integer> nullInAttendanceDays = Arrays.asList(0, 0, 0, 1, 0, 1, null, 2, 2);

            @Test
            @DisplayName("예외를 던진다.")
            void it_returns_oneseo() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> OneseoService.calcAbsentDaysCount(nullInAbsentDays, nullInAttendanceDays));

                assertEquals("결석 횟수나 지각, 조퇴, 결과 횟수에 null 값이 포함되어 있습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("isValidMiddleSchoolInfo 메소드는")
    class Describe_isValidMiddleSchoolInfo {

        private final OneseoReqDto validCandidateReqDto = OneseoReqDto.builder().schoolName("금호중앙중학교")
                .schoolAddress("어딘가").schoolTeacherName("김선생").schoolTeacherPhoneNumber("01000000000")
                .graduationType(CANDIDATE).build();

        private final OneseoReqDto validGedReqDto = OneseoReqDto.builder().schoolName("").schoolAddress("")
                .schoolTeacherName("").schoolTeacherPhoneNumber("").graduationType(GED).build();

        private final OneseoReqDto invalidCandidateReqDto = OneseoReqDto.builder().schoolName("").schoolAddress("")
                .schoolTeacherName("").schoolTeacherPhoneNumber("").graduationType(CANDIDATE).build();

        @Nested
        @DisplayName("졸업 예정자(CANDIDATE)이고 모든 중학교 정보가 유효하면")
        class Context_with_valid_candidate {

            @Test
            @DisplayName("예외를 던지지 않는다.")
            void it_does_not_throw_exception() {
                OneseoService.isValidMiddleSchoolInfo(validCandidateReqDto);
            }
        }

        @Nested
        @DisplayName("검정고시 지원자(GED)이고 중학교 정보를 작성하지 않았다면")
        class Context_with_valid_ged {

            @Test
            @DisplayName("예외를 던지지 않는다.")
            void it_does_not_throw_exception() {
                OneseoService.isValidMiddleSchoolInfo(validGedReqDto);
            }
        }

        @Nested
        @DisplayName("졸업 예정자(CANDIDATE)이고 필수 중학교 정보가 비어 있으면")
        class Context_with_invalid_candidate {

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> OneseoService.isValidMiddleSchoolInfo(invalidCandidateReqDto));

                assertEquals("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("assignSubmitCode 메소드는")
    class Describe_assignSubmitCode {

        private final int maxSubmitCodeNumber = 10;
        private final Oneseo oneseo = mock(Oneseo.class);

        @Nested
        @DisplayName("일반전형 원서가 주어지면")
        class Context_with_existing_member_and_oneseo {

            @BeforeEach
            void setUp() {
                given(oneseo.getWantedScreening()).willReturn(GENERAL);
                given(oneseoRepository.findMaxSubmitCodeByScreening(oneseo.getWantedScreening().getScreeningCategory()))
                        .willReturn(maxSubmitCodeNumber);
            }

            @Test
            @DisplayName("A-N 번대의 접수번호가 생성된다.")
            void it_returns_oneseo() {
                oneseoService.assignSubmitCode(oneseo, null);
                verify(oneseo).setOneseoSubmitCode("A-" + (maxSubmitCodeNumber + 1));
            }
        }
    }

    @Nested
    @DisplayName("buildCalcDtoWithFillEmpty 메소드는")
    class Describe_buildCalcDtoWithFillEmpty {
        private MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto;

        @BeforeEach
        void setUpMiddleSchoolAchievementReqDto() {
            middleSchoolAchievementReqDto = createDefaultDtoBuilder().build();
        }

        @Nested
        @DisplayName("검정고시 지원자(GED)이면")
        class Context_with_ged {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().gedAvgScore(new BigDecimal("100")).build();
                graduationType = GED;
            }

            @Test
            @DisplayName("검정고시 평균 점수만 포함된 DTO를 반환한다.")
            void it_returns_dto_with_ged_avg_score() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(new BigDecimal("100"), resultDto.gedAvgScore());
                assertNull(resultDto.achievement1_2());
                assertNull(resultDto.achievement2_1());
                assertNull(resultDto.achievement2_2());
                assertNull(resultDto.achievement3_1());
                assertNull(resultDto.achievement3_2());
            }
        }

        @Nested
        @DisplayName("졸업자(GRADUATE)이고 3학년 2학기 성적이 null이면")
        class Context_with_graduate_and_missing_3_2 {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().freeSemester("3-2").achievement3_2(null)
                        .build();
                graduationType = GraduationType.GRADUATE;
            }

            @Test
            @DisplayName("3학년 2학기 성적을 3학년 1학기 성적으로 채운 DTO를 반환한다.")
            void it_fills_3_2_with_3_1() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(resultDto.achievement1_2(), middleSchoolAchievementReqDto.achievement1_2());
                assertEquals(resultDto.achievement2_1(), middleSchoolAchievementReqDto.achievement2_1());
                assertEquals(resultDto.achievement2_2(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement3_1(), middleSchoolAchievementReqDto.achievement3_1());
                assertEquals(resultDto.achievement3_2(), middleSchoolAchievementReqDto.achievement3_1());
                assertNull(resultDto.gedAvgScore());
            }
        }

        @Nested
        @DisplayName("3학년 1학기 성적이 null이면")
        class Context_with_missing_3_1 {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().achievement3_1(null).build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("3학년 1학기 성적을 3학년 2학기 성적으로 채운 DTO를 반환한다.")
            void it_fills_3_1_with_3_2() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(resultDto.achievement1_2(), middleSchoolAchievementReqDto.achievement1_2());
                assertEquals(resultDto.achievement2_1(), middleSchoolAchievementReqDto.achievement2_1());
                assertEquals(resultDto.achievement2_2(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement3_1(), middleSchoolAchievementReqDto.achievement3_2());
                assertEquals(resultDto.achievement3_2(), middleSchoolAchievementReqDto.achievement3_2());
                assertNull(resultDto.gedAvgScore());
            }
        }

        @Nested
        @DisplayName("2학년 1학기 성적이 null이면")
        class Context_with_missing_2_1 {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().achievement2_1(null).build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("2학년 1학기 성적을 2학년 2학기 성적으로 채운 DTO를 반환한다.")
            void it_fills_2_1_with_2_2() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(resultDto.achievement1_2(), middleSchoolAchievementReqDto.achievement1_2());
                assertEquals(resultDto.achievement2_1(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement2_2(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement3_1(), middleSchoolAchievementReqDto.achievement3_1());
                assertEquals(resultDto.achievement3_2(), middleSchoolAchievementReqDto.achievement3_2());
                assertNull(resultDto.gedAvgScore());
            }
        }

        @Nested
        @DisplayName("2학년 2학기 성적이 null이면")
        class Context_with_missing_2_2 {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().achievement2_2(null).build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("2학년 2학기 성적을 2학년 1학기 성적으로 채운 DTO를 반환한다.")
            void it_fills_2_2_with_2_1() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(resultDto.achievement1_2(), middleSchoolAchievementReqDto.achievement1_2());
                assertEquals(resultDto.achievement2_1(), middleSchoolAchievementReqDto.achievement2_1());
                assertEquals(resultDto.achievement2_2(), middleSchoolAchievementReqDto.achievement2_1());
                assertEquals(resultDto.achievement3_1(), middleSchoolAchievementReqDto.achievement3_1());
                assertEquals(resultDto.achievement3_2(), middleSchoolAchievementReqDto.achievement3_2());
                assertNull(resultDto.gedAvgScore());
            }
        }

        @Nested
        @DisplayName("졸업예정자(CANDIDATE)이고, 1학년 2학기 성적만 null이면")
        class Context_with_candidate_and_missing_1_2_only {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().achievement1_2(null).build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("1학년 2학기 성적을 1학년 1학기 성적으로 채운 DTO를 반환한다.")
            void it_fills_1_2_with_1_1() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(resultDto.achievement1_2(), middleSchoolAchievementReqDto.achievement1_1());
                assertEquals(resultDto.achievement2_1(), middleSchoolAchievementReqDto.achievement2_1());
                assertEquals(resultDto.achievement2_2(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement3_1(), middleSchoolAchievementReqDto.achievement3_1());
                assertEquals(resultDto.achievement3_2(), middleSchoolAchievementReqDto.achievement3_2());
                assertNull(resultDto.gedAvgScore());
            }
        }

        @Nested
        @DisplayName("졸업예정자(CANDIDATE)이고, 1학년 성적이 모두 null이면")
        class Context_with_candidate_and_missing_1_1_and_1_2 {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder().achievement1_1(null).achievement1_2(null)
                        .build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("1학년 2학기 성적을 2학년 2학기 성적으로 채운 DTO를 반환한다.")
            void it_fills_1_2_with_2_2() {
                MiddleSchoolAchievementCalcDto resultDto = OneseoService
                        .buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType);
                assertEquals(resultDto.achievement1_2(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement2_1(), middleSchoolAchievementReqDto.achievement2_1());
                assertEquals(resultDto.achievement2_2(), middleSchoolAchievementReqDto.achievement2_2());
                assertEquals(resultDto.achievement3_1(), middleSchoolAchievementReqDto.achievement3_1());
                assertEquals(resultDto.achievement3_2(), middleSchoolAchievementReqDto.achievement3_2());
                assertNull(resultDto.gedAvgScore());
            }
        }

        @Nested
        @DisplayName("일반교과 성적에 잘못된 등급이 포함되어 있으면")
        class Context_with_invalid_general_achievement {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder()
                        .achievement2_1(new ArrayList<>(List.of(1, 2, 6))) // valid value: 1~5
                        .build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> OneseoService.buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType));

                assertEquals("올바르지 않은 일반교과 등급이 입력되었습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("예체능 성적에 잘못된 등급이 포함되어 있으면")
        class Context_with_invalid_arts_physical_achievement {
            private GraduationType graduationType;

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = createDefaultDtoBuilder()
                        .artsPhysicalAchievement(new ArrayList<>(List.of(3, 4, 2))) // valid value: 3~5
                        .build();
                graduationType = CANDIDATE;
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> OneseoService.buildCalcDtoWithFillEmpty(middleSchoolAchievementReqDto, graduationType));

                assertEquals("올바르지 않은 예체능 등급이 입력되었습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        private MiddleSchoolAchievementReqDto.MiddleSchoolAchievementReqDtoBuilder createDefaultDtoBuilder() {
            return MiddleSchoolAchievementReqDto.builder()
                    .achievement1_1(new ArrayList<>(List.of(1, 1, 1, 1, 1, 1, 1, 1, 1)))
                    .achievement1_2(new ArrayList<>(List.of(1, 1, 1, 1, 2, 2, 2, 2, 2)))
                    .achievement2_1(new ArrayList<>(List.of(2, 2, 2, 2, 1, 1, 1, 1, 1)))
                    .achievement2_2(new ArrayList<>(List.of(2, 2, 2, 2, 2, 2, 2, 2, 2)))
                    .achievement3_1(new ArrayList<>(List.of(3, 3, 3, 3, 1, 1, 1, 1, 1)))
                    .achievement3_2(new ArrayList<>(List.of(3, 3, 3, 3, 2, 2, 2, 2, 2)))
                    .artsPhysicalAchievement(new ArrayList<>(List.of(3, 4, 5, 3, 4, 5, 3, 4, 5)))
                    .absentDays(new ArrayList<>(List.of(0, 1, 2)))
                    .attendanceDays(new ArrayList<>(List.of(0, 1, 2, 3, 4, 5)))
                    .volunteerTime(new ArrayList<>(List.of(10, 20, 30))).gedAvgScore(null);
        }
    }
}
