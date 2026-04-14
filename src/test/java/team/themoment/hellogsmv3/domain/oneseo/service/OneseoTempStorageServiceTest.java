package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

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
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoTempReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.FoundOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("OneseoTempStorageService 클래스의")
public class OneseoTempStorageServiceTest {

    @Mock
    private MemberService memberService;
    @Mock
    private OneseoRepository oneseoRepository;
    @InjectMocks
    private OneseoTempStorageService oneseoTempStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final Integer step = 1;
        private final OneseoTempReqDto reqDto = OneseoTempReqDto.builder()
                // 필요한 필드를 설정
                .graduationType(GraduationType.GRADUATE).graduationDate("2024-01").address("광주광역시 광산구 송정동 상무대로 312")
                .detailAddress("101동 1001호").guardianName("김보호").guardianPhoneNumber("01012345678")
                .relationshipWithGuardian("모").schoolName("금호중앙중학교").schoolAddress("광주광역시 북구 운암2동 금호로 100")
                .schoolTeacherName("김선생").schoolTeacherPhoneNumber("01012345678")
                .profileImg("https://example.com/image.jpg")
                .middleSchoolAchievement(MiddleSchoolAchievementReqDto.builder().achievement1_2(null)
                        .achievement2_1(List.of(4, 5, 3, 5, 4, 5, 3, 5, 2))
                        .achievement2_2(List.of(5, 2, 5, 5, 4, 1, 5, 5, 0))
                        .achievement3_1(List.of(3, 5, 3, 5, 1, 3, 5, 2, 0)).achievement3_2(null)
                        .generalSubjects(List.of("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어"))
                        .newSubjects(List.of("프로그래밍")).artsPhysicalAchievement(List.of(5, 4, 5, 3, 5, 0, 5, 3, 5))
                        .artsPhysicalSubjects(List.of("체육", "미술", "음악")).absentDays(List.of(2, 0, 0))
                        .attendanceDays(List.of(0, 4, 0, 0, 0, 0, 0, 0, 0)).volunteerTime(List.of(7, 3, 4))
                        .liberalSystem("자유학년제").freeSemester(null).gedAvgScore(null).build())
                .firstDesiredMajor(Major.SW).secondDesiredMajor(Major.AI).thirdDesiredMajor(Major.IOT).build();

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지고")
        class Context_with_existing_member_id {
            private Member member;

            @BeforeEach
            void setUp() {
                member = Member.builder().id(memberId).name("홍길동").sex(Sex.MALE).birth(LocalDate.of(2009, 1, 1))
                        .phoneNumber("01012345678").build();

                given(memberService.findByIdOrThrow(memberId)).willReturn(member);
            }

            @Nested
            @DisplayName("회원이 원서를 제출하지 않았다면")
            class Oneseo_not_exist {
                @BeforeEach
                void setUp() {
                    given(oneseoRepository.existsByMember(member)).willReturn(false);
                }

                @Test
                @DisplayName("FoundOneseoResDto를 반환한다.")
                void it_returns_found_oneseo_res_dto() {
                    FoundOneseoResDto result = oneseoTempStorageService.execute(reqDto, step, memberId);
                    assertDto(result);
                }

                private void assertDto(FoundOneseoResDto resDto) {
                    assertNull(resDto.oneseoId());
                    assertNull(resDto.submitCode());
                    assertEquals(reqDto.screening(), resDto.wantedScreening());
                    assertEquals(reqDto.firstDesiredMajor(), resDto.desiredMajors().firstDesiredMajor());
                    assertEquals(reqDto.secondDesiredMajor(), resDto.desiredMajors().secondDesiredMajor());
                    assertEquals(reqDto.thirdDesiredMajor(), resDto.desiredMajors().thirdDesiredMajor());
                    assertEquals(reqDto.graduationType(), resDto.privacyDetail().graduationType());
                    assertEquals(reqDto.graduationDate(), resDto.privacyDetail().graduationDate());
                    assertEquals(reqDto.address(), resDto.privacyDetail().address());
                    assertEquals(reqDto.detailAddress(), resDto.privacyDetail().detailAddress());
                    assertEquals(reqDto.guardianName(), resDto.privacyDetail().guardianName());
                    assertEquals(reqDto.guardianPhoneNumber(), resDto.privacyDetail().guardianPhoneNumber());
                    assertEquals(reqDto.relationshipWithGuardian(), resDto.privacyDetail().relationshipWithGuardian());
                    assertEquals(reqDto.schoolName(), resDto.privacyDetail().schoolName());
                    assertEquals(reqDto.schoolAddress(), resDto.privacyDetail().schoolAddress());
                    assertEquals(reqDto.schoolTeacherName(), resDto.privacyDetail().schoolTeacherName());
                    assertEquals(reqDto.schoolTeacherPhoneNumber(), resDto.privacyDetail().schoolTeacherPhoneNumber());
                    assertEquals(reqDto.profileImg(), resDto.privacyDetail().profileImg());
                    assertEquals(reqDto.studentNumber(), resDto.privacyDetail().studentNumber());
                    assertEquals(reqDto.middleSchoolAchievement().achievement1_2(),
                            resDto.middleSchoolAchievement().achievement1_2());
                    assertEquals(reqDto.middleSchoolAchievement().achievement2_1(),
                            resDto.middleSchoolAchievement().achievement2_1());
                    assertEquals(reqDto.middleSchoolAchievement().achievement2_2(),
                            resDto.middleSchoolAchievement().achievement2_2());
                    assertEquals(reqDto.middleSchoolAchievement().achievement3_1(),
                            resDto.middleSchoolAchievement().achievement3_1());
                    assertEquals(reqDto.middleSchoolAchievement().achievement3_2(),
                            resDto.middleSchoolAchievement().achievement3_2());
                    assertEquals(reqDto.middleSchoolAchievement().generalSubjects(),
                            resDto.middleSchoolAchievement().generalSubjects());
                    assertEquals(reqDto.middleSchoolAchievement().newSubjects(),
                            resDto.middleSchoolAchievement().newSubjects());
                    assertEquals(reqDto.middleSchoolAchievement().artsPhysicalAchievement(),
                            resDto.middleSchoolAchievement().artsPhysicalAchievement());
                    assertEquals(reqDto.middleSchoolAchievement().artsPhysicalSubjects(),
                            resDto.middleSchoolAchievement().artsPhysicalSubjects());
                    assertEquals(reqDto.middleSchoolAchievement().absentDays(),
                            resDto.middleSchoolAchievement().absentDays());
                    assertEquals(reqDto.middleSchoolAchievement().attendanceDays(),
                            resDto.middleSchoolAchievement().attendanceDays());
                    assertEquals(reqDto.middleSchoolAchievement().volunteerTime(),
                            resDto.middleSchoolAchievement().volunteerTime());
                    assertEquals(reqDto.middleSchoolAchievement().liberalSystem(),
                            resDto.middleSchoolAchievement().liberalSystem());
                    assertEquals(reqDto.middleSchoolAchievement().freeSemester(),
                            resDto.middleSchoolAchievement().freeSemester());
                    assertEquals(reqDto.middleSchoolAchievement().gedAvgScore(),
                            resDto.middleSchoolAchievement().gedAvgScore());
                    assertEquals(step, resDto.step());
                }
            }

            @Nested
            @DisplayName("회원이 원서를 제출했다면")
            class Oneseo_exist {
                @BeforeEach
                void setUp() {
                    given(oneseoRepository.existsByMember(member)).willReturn(true);
                }

                @Test
                @DisplayName("ExpectedException을 던진다.")
                void it_throws_expected_exception() {
                    ExpectedException exception = assertThrows(ExpectedException.class,
                            () -> oneseoTempStorageService.execute(reqDto, step, memberId));

                    assertEquals("이미 원서 제출을 하였습니다.", exception.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                given(memberService.findByIdOrThrow(memberId)).willThrow(
                        new ExpectedException("존재하지 않는 회원입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> oneseoTempStorageService.execute(reqDto, step, memberId));

                assertEquals("존재하지 않는 회원입니다. member ID: " + memberId, exception.getMessage());
            }
        }
    }
}
