package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.YES;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("ModifyPersonalInfoService 클래스의")
class ModifyPersonalInfoServiceTest {

    @Mock
    private OneseoService oneseoService;
    @Mock
    private OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;

    @InjectMocks
    private ModifyPersonalInfoService modifyPersonalInfoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;

        private ModifyPersonalInfoReqDto buildReqDto(GraduationType graduationType,
                String schoolTeacherName,
                String schoolTeacherPhoneNumber,
                String schoolName,
                String schoolAddress) {
            return ModifyPersonalInfoReqDto.builder().name("홍길동").birth(LocalDate.of(2009, 1, 1)).sex(Sex.MALE)
                    .guardianName("김보호").guardianPhoneNumber("01000000001").relationshipWithGuardian("모")
                    .profileImg("https://abc.com/img.jpg").address("광주광역시 광산구 송정동 상무대로 312").detailAddress("101동 1001호")
                    .graduationType(graduationType).schoolTeacherName(schoolTeacherName)
                    .schoolTeacherPhoneNumber(schoolTeacherPhoneNumber).schoolName(schoolName)
                    .schoolAddress(schoolAddress).graduationDate("2025-02").studentNumber("30508").build();
        }

        @Nested
        @DisplayName("졸업예정(CANDIDATE)이고 중학교 정보가 모두 입력된 경우")
        class Context_with_candidate_and_full_school_info {

            private Oneseo oneseo;
            private Member member;

            @BeforeEach
            void setUp() {
                member = mock(Member.class);
                oneseo = Oneseo.builder().id(1L).member(member)
                        .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(null).build()).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
            }

            @Test
            @DisplayName("OneseoPrivacyDetail을 저장하고 회원 정보를 수정한다")
            void it_saves_privacy_detail_and_modifies_member() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto(CANDIDATE,
                        "김선생",
                        "01000000002",
                        "금호중앙중학교",
                        "광주광역시 북구 운암2동");

                modifyPersonalInfoService.execute(reqDto, memberId);

                ArgumentCaptor<OneseoPrivacyDetail> captor = ArgumentCaptor.forClass(OneseoPrivacyDetail.class);
                verify(oneseoPrivacyDetailRepository).save(captor.capture());

                OneseoPrivacyDetail saved = captor.getValue();
                assertEquals(oneseo.getId(), saved.getId());
                assertEquals(oneseo, saved.getOneseo());
                assertEquals(CANDIDATE, saved.getGraduationType());
                assertEquals("2025-02", saved.getGraduationDate());
                assertEquals("광주광역시 광산구 송정동 상무대로 312", saved.getAddress());
                assertEquals("101동 1001호", saved.getDetailAddress());
                assertEquals("https://abc.com/img.jpg", saved.getProfileImg());
                assertEquals("김보호", saved.getGuardianName());
                assertEquals("01000000001", saved.getGuardianPhoneNumber());
                assertEquals("모", saved.getRelationshipWithGuardian());
                assertEquals("김선생", saved.getSchoolTeacherName());
                assertEquals("01000000002", saved.getSchoolTeacherPhoneNumber());
                assertEquals("금호중앙중학교", saved.getSchoolName());
                assertEquals("광주광역시 북구 운암2동", saved.getSchoolAddress());
                assertEquals("30508", saved.getStudentNumber());

                verify(member).modifyMember("홍길동", LocalDate.of(2009, 1, 1), member.getPhoneNumber(), Sex.MALE);
            }
        }

        @Nested
        @DisplayName("졸업(GRADUATE) 타입이고 중학교 정보가 없는 경우")
        class Context_with_graduate_and_no_school_info {

            @BeforeEach
            void setUp() {
                Member member = mock(Member.class);
                Oneseo oneseo = Oneseo.builder().id(1L).member(member)
                        .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(null).build()).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
            }

            @Test
            @DisplayName("중학교 정보 검증 없이 OneseoPrivacyDetail을 저장한다")
            void it_saves_privacy_detail_without_school_info_validation() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto(GRADUATE, null, null, null, null);

                modifyPersonalInfoService.execute(reqDto, memberId);

                verify(oneseoPrivacyDetailRepository).save(any(OneseoPrivacyDetail.class));
            }
        }

        @Nested
        @DisplayName("졸업예정(CANDIDATE)이지만 중학교 정보가 누락된 경우")
        class Context_with_candidate_and_missing_school_info {

            @BeforeEach
            void setUp() {
                Member member = mock(Member.class);
                Oneseo oneseo = Oneseo.builder().id(1L).member(member)
                        .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(null).build()).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
            }

            @Test
            @DisplayName("schoolTeacherName이 없으면 ExpectedException을 던진다")
            void it_throws_when_school_teacher_name_is_blank() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto(CANDIDATE,
                        null,
                        "01000000002",
                        "금호중앙중학교",
                        "광주광역시 북구 운암2동");

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId));

                assertEquals("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }

            @Test
            @DisplayName("schoolName이 없으면 ExpectedException을 던진다")
            void it_throws_when_school_name_is_blank() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto(CANDIDATE, "김선생", "01000000002", null, "광주광역시 북구 운암2동");

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId));

                assertEquals("중학교 졸업예정인 지원자는 현재 재학 중인 중학교 정보를 필수로 입력해야 합니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 전형 결과가 이미 산출된 경우")
        class Context_when_first_test_result_already_announced {

            @BeforeEach
            void setUp() {
                Member member = mock(Member.class);
                Oneseo oneseo = Oneseo.builder().id(1L).member(member)
                        .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(YES).build()).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto(CANDIDATE,
                        "김선생",
                        "01000000002",
                        "금호중앙중학교",
                        "광주광역시 북구 운암2동");

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId));

                assertEquals("1차 전형 결과 산출 이후에는 작업을 진행할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("회원 ID가 유효하지 않은 경우")
        class Context_with_invalid_member_id {

            @BeforeEach
            void setUp() {
                doThrow(new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND))
                        .when(oneseoService).findWithMemberByMemberIdOrThrow(memberId);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto(CANDIDATE,
                        "김선생",
                        "01000000002",
                        "금호중앙중학교",
                        "광주광역시 북구 운암2동");

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
