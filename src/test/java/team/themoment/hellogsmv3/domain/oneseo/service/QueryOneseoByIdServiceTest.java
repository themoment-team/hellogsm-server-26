package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.repository.MiddleSchoolAchievementRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("QueryOneseoByIdService 클래스의")
class QueryOneseoByIdServiceTest {

    @Mock
    private OneseoService oneseoService;

    @Mock
    private MemberService memberService;

    @Mock
    private OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;

    @Mock
    private MiddleSchoolAchievementRepository middleSchoolAchievementRepository;

    @InjectMocks
    private QueryOneseoByIdService queryOneseoByIdService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;

        @Nested
        @DisplayName("존재하는 회원 ID가 주어졌을 때")
        class Context_with_existing_member_id {

            private Member member;
            private Oneseo oneseo;
            private OneseoPrivacyDetail oneseoPrivacyDetail;
            private MiddleSchoolAchievement middleSchoolAchievement;

            void setUp_it_returns_oneseo() {
                member = buildMember(memberId);
                oneseoPrivacyDetail = buildOneseoPrivacyDetail();
                middleSchoolAchievement = buildMiddleSchoolAchievement();
                oneseo = buildOneseo(member, middleSchoolAchievement, oneseoPrivacyDetail);

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseoPrivacyDetailRepository.findByOneseo(oneseo)).willReturn(oneseoPrivacyDetail);
                given(middleSchoolAchievementRepository.findByOneseo(oneseo)).willReturn(middleSchoolAchievement);
            }

            @Test
            @DisplayName("원서가 존재한다면 원서 관련 정보(개인정보, 중학교성취도, 전형 등)를 반환한다")
            void it_returns_oneseo() {
                setUp_it_returns_oneseo();

                FoundOneseoResDto result = queryOneseoByIdService.execute(memberId);

                assertEquals(oneseo.getId(), result.oneseoId());
                assertEquals(oneseo.getOneseoSubmitCode(), result.submitCode());
                assertEquals(oneseo.getWantedScreening(), result.wantedScreening());

                DesiredMajorsResDto desiredMajorsResDto = result.desiredMajors();
                assertEquals(oneseo.getDesiredMajors().getFirstDesiredMajor(), desiredMajorsResDto.firstDesiredMajor());
                assertEquals(oneseo.getDesiredMajors().getSecondDesiredMajor(),
                        desiredMajorsResDto.secondDesiredMajor());
                assertEquals(oneseo.getDesiredMajors().getThirdDesiredMajor(), desiredMajorsResDto.thirdDesiredMajor());

                OneseoPrivacyDetailResDto oneseoPrivacyDetailResDto = result.privacyDetail();
                assertEquals(member.getName(), oneseoPrivacyDetailResDto.name());
                assertEquals(member.getSex(), oneseoPrivacyDetailResDto.sex());
                assertEquals(member.getBirth(), oneseoPrivacyDetailResDto.birth());
                assertEquals(member.getPhoneNumber(), oneseoPrivacyDetailResDto.phoneNumber());
                assertEquals(oneseoPrivacyDetail.getGraduationType(), oneseoPrivacyDetailResDto.graduationType());
                assertEquals(oneseoPrivacyDetail.getGraduationDate(), oneseoPrivacyDetailResDto.graduationDate());
                assertEquals(oneseoPrivacyDetail.getAddress(), oneseoPrivacyDetailResDto.address());
                assertEquals(oneseoPrivacyDetail.getDetailAddress(), oneseoPrivacyDetailResDto.detailAddress());
                assertEquals(oneseoPrivacyDetail.getGuardianName(), oneseoPrivacyDetailResDto.guardianName());
                assertEquals(oneseoPrivacyDetail.getGuardianPhoneNumber(),
                        oneseoPrivacyDetailResDto.guardianPhoneNumber());
                assertEquals(oneseoPrivacyDetail.getRelationshipWithGuardian(),
                        oneseoPrivacyDetailResDto.relationshipWithGuardian());
                assertEquals(oneseoPrivacyDetail.getSchoolName(), oneseoPrivacyDetailResDto.schoolName());
                assertEquals(oneseoPrivacyDetail.getSchoolAddress(), oneseoPrivacyDetailResDto.schoolAddress());
                assertEquals(oneseoPrivacyDetail.getSchoolTeacherName(), oneseoPrivacyDetailResDto.schoolTeacherName());
                assertEquals(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber(),
                        oneseoPrivacyDetailResDto.schoolTeacherPhoneNumber());
                assertEquals(oneseoPrivacyDetail.getProfileImg(), oneseoPrivacyDetailResDto.profileImg());
                assertEquals(oneseoPrivacyDetail.getStudentNumber(), oneseoPrivacyDetailResDto.studentNumber());

                MiddleSchoolAchievementResDto middleSchoolAchievementResDto = result.middleSchoolAchievement();
                assertEquals(middleSchoolAchievement.getAchievement1_2(),
                        middleSchoolAchievementResDto.achievement1_2());
                assertEquals(middleSchoolAchievement.getAchievement2_1(),
                        middleSchoolAchievementResDto.achievement2_1());
                assertEquals(middleSchoolAchievement.getAchievement2_2(),
                        middleSchoolAchievementResDto.achievement2_2());
                assertEquals(middleSchoolAchievement.getAchievement3_1(),
                        middleSchoolAchievementResDto.achievement3_1());
                assertEquals(middleSchoolAchievement.getAchievement3_2(),
                        middleSchoolAchievementResDto.achievement3_2());
                assertEquals(middleSchoolAchievement.getGeneralSubjects(),
                        middleSchoolAchievementResDto.generalSubjects());
                assertEquals(middleSchoolAchievement.getNewSubjects(), middleSchoolAchievementResDto.newSubjects());
                assertEquals(middleSchoolAchievement.getArtsPhysicalAchievement(),
                        middleSchoolAchievementResDto.artsPhysicalAchievement());
                assertEquals(middleSchoolAchievement.getArtsPhysicalSubjects(),
                        middleSchoolAchievementResDto.artsPhysicalSubjects());
                assertEquals(middleSchoolAchievement.getAbsentDays(), middleSchoolAchievementResDto.absentDays());
                assertEquals(middleSchoolAchievement.getAttendanceDays(),
                        middleSchoolAchievementResDto.attendanceDays());
                assertEquals(middleSchoolAchievement.getVolunteerTime(), middleSchoolAchievementResDto.volunteerTime());
                assertEquals(middleSchoolAchievement.getLiberalSystem(), middleSchoolAchievementResDto.liberalSystem());
                assertEquals(middleSchoolAchievement.getFreeSemester(), middleSchoolAchievementResDto.freeSemester());
                assertEquals(middleSchoolAchievement.getGedAvgScore(), middleSchoolAchievementResDto.gedAvgScore());
            }

            void setUp_it_throws_expected_exception() {
                member = buildMember(memberId);

                when(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).thenThrow(
                        new ExpectedException("원서를 찾을 수 없습니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("원서가 존재하지 않는다면 ExpectedException을 던진다")
            void it_throws_expected_exception() {
                setUp_it_throws_expected_exception();

                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    queryOneseoByIdService.execute(memberId);
                });

                assertEquals("원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                when(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).thenThrow(
                        new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    queryOneseoByIdService.execute(memberId);
                });

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }

    private Member buildMember(Long memberId) {
        return Member.builder().id(memberId).name("최장우").sex(Sex.MALE).birth(LocalDate.of(2006, 3, 6))
                .phoneNumber("01012345678").build();
    }

    private EntranceTestFactorsDetail buildEntranceTestFactorsDetail() {
        return EntranceTestFactorsDetail.builder().build();
    }

    private EntranceTestResult buildEntranceTestResult() {
        return EntranceTestResult.builder().entranceTestFactorsDetail(buildEntranceTestFactorsDetail()).build();
    }

    private Oneseo buildOneseo(Member member,
            MiddleSchoolAchievement middleSchoolAchievement,
            OneseoPrivacyDetail oneseoPrivacyDetail) {
        return Oneseo.builder().member(member).id(1L).oneseoSubmitCode("submitCode").wantedScreening(Screening.GENERAL)
                .desiredMajors(new DesiredMajors(Major.SW, Major.IOT, Major.AI))
                .entranceTestResult(buildEntranceTestResult()).middleSchoolAchievement(middleSchoolAchievement)
                .oneseoPrivacyDetail(oneseoPrivacyDetail).build();
    }

    private OneseoPrivacyDetail buildOneseoPrivacyDetail() {
        return OneseoPrivacyDetail.builder().graduationType(GraduationType.GRADUATE).graduationDate("2020-02")
                .address("거주 주소").detailAddress("상세 주소").guardianName("홍길동").guardianPhoneNumber("01087654321")
                .relationshipWithGuardian("부").schoolName("양산중학교").schoolAddress("학교 주소").schoolTeacherName("김철수")
                .schoolTeacherPhoneNumber("01012341234").profileImg("https://example.com").build();
    }

    private MiddleSchoolAchievement buildMiddleSchoolAchievement() {
        List<Integer> integerList = List.of(1, 2, 3, 4, 5);
        List<String> stringList = List.of("과목1", "과목2", "과목3");
        BigDecimal bigDecimal = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);

        return MiddleSchoolAchievement.builder().achievement1_2(integerList).achievement2_1(integerList)
                .achievement2_2(integerList).achievement3_1(integerList).achievement3_2(integerList)
                .generalSubjects(stringList).newSubjects(stringList)
                .artsPhysicalAchievement(List.of(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)).artsPhysicalSubjects(stringList)
                .absentDays(integerList).attendanceDays(integerList).volunteerTime(integerList).liberalSystem("자유학년제")
                .freeSemester(null).gedAvgScore(bigDecimal).build();
    }
}
