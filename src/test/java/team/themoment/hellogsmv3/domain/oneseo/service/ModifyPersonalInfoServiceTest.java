package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("ModifyPersonalInfoService 클래스의")
class ModifyPersonalInfoServiceTest {

    @Mock
    private OneseoService oneseoService;

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

        private ModifyPersonalInfoReqDto buildReqDto(String name, LocalDate birth, Sex sex) {
            return ModifyPersonalInfoReqDto.builder().name(name).birth(birth).sex(sex).build();
        }

        @Nested
        @DisplayName("1차 전형 결과가 산출되지 않은 경우")
        class Context_with_first_test_not_announced {

            private Member member;
            private Oneseo oneseo;

            @BeforeEach
            void setUp() {
                member = mock(Member.class);
                oneseo = mock(Oneseo.class);
                EntranceTestResult entranceTestResult = mock(EntranceTestResult.class);

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseo.getMember()).willReturn(member);
                given(oneseo.getEntranceTestResult()).willReturn(entranceTestResult);
                given(entranceTestResult.getFirstTestPassYn()).willReturn(null);
            }

            @Test
            @DisplayName("회원의 이름, 생년월일, 성별을 수정한다")
            void it_modifies_member_personal_info() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto("홍길동", LocalDate.of(2009, 1, 1), Sex.MALE);

                modifyPersonalInfoService.execute(reqDto, memberId, true);

                verify(member).modifyMember("홍길동", LocalDate.of(2009, 1, 1), member.getPhoneNumber(), Sex.MALE);
            }
        }

        @Nested
        @DisplayName("1차 전형 결과가 산출된 후 일반 사용자가 수정을 시도하는 경우")
        class Context_with_first_test_announced {

            @BeforeEach
            void setUp() {
                Oneseo oneseo = mock(Oneseo.class);
                EntranceTestResult entranceTestResult = mock(EntranceTestResult.class);

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseo.getEntranceTestResult()).willReturn(entranceTestResult);
                given(entranceTestResult.getFirstTestPassYn()).willReturn(YesNo.YES);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto("홍길동", LocalDate.of(2009, 1, 1), Sex.MALE);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId, true));

                assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("관리자가 1차 전형 결과 산출 이후 수정을 시도하는 경우")
        class Context_with_admin_after_first_test {

            private Member member;

            @BeforeEach
            void setUp() {
                member = mock(Member.class);
                Oneseo oneseo = mock(Oneseo.class);

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseo.getMember()).willReturn(member);
            }

            @Test
            @DisplayName("검증 없이 회원 정보를 수정한다")
            void it_modifies_member_personal_info_without_check() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto("홍길동", LocalDate.of(2009, 1, 1), Sex.MALE);

                modifyPersonalInfoService.execute(reqDto, memberId, false);

                verify(member).modifyMember("홍길동", LocalDate.of(2009, 1, 1), member.getPhoneNumber(), Sex.MALE);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어진 경우")
        class Context_with_nonexistent_member_id {

            @BeforeEach
            void setUp() {
                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willThrow(
                        new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto("홍길동", LocalDate.of(2009, 1, 1), Sex.MALE);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId, true));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
