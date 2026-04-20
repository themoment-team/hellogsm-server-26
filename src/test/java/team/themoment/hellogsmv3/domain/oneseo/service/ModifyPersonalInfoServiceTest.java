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
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ModifyPersonalInfoReqDto;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("ModifyPersonalInfoService 클래스의")
class ModifyPersonalInfoServiceTest {

    @Mock
    private MemberService memberService;

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
        @DisplayName("유효한 회원 ID와 인적사항 데이터가 주어진 경우")
        class Context_with_valid_member_id_and_data {

            private Member member;

            @BeforeEach
            void setUp() {
                member = mock(Member.class);
                given(memberService.findByIdForUpdateOrThrow(memberId)).willReturn(member);
            }

            @Test
            @DisplayName("회원의 이름, 생년월일, 성별을 수정한다")
            void it_modifies_member_personal_info() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto("홍길동", LocalDate.of(2009, 1, 1), Sex.MALE);

                modifyPersonalInfoService.execute(reqDto, memberId);

                verify(member).modifyMember("홍길동", LocalDate.of(2009, 1, 1), member.getPhoneNumber(), Sex.MALE);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어진 경우")
        class Context_with_nonexistent_member_id {

            @BeforeEach
            void setUp() {
                doThrow(new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND))
                        .when(memberService).findByIdForUpdateOrThrow(memberId);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ModifyPersonalInfoReqDto reqDto = buildReqDto("홍길동", LocalDate.of(2009, 1, 1), Sex.MALE);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyPersonalInfoService.execute(reqDto, memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
