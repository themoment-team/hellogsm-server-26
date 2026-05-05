package team.themoment.hellogsmv3.domain.common.utility.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("DeleteMemberService 클래스의")
class DeleteMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OneseoRepository oneseoRepository;
    @Mock
    private CodeRepository codeRepository;

    @InjectMocks
    private DeleteMemberService deleteMemberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final String phoneNumber = "01012345678";
        private final Long memberId = 1L;

        @Nested
        @DisplayName("유효한 전화번호가 주어지면")
        class Context_with_valid_phone_number {

            private Member existingMember;
            private Oneseo existingOneseo;
            private AuthenticationCode existingAuthCode;

            @BeforeEach
            void setUp() {
                existingMember = mock(Member.class);
                existingOneseo = mock(Oneseo.class);
                existingAuthCode = new AuthenticationCode(memberId,
                        "123456",
                        phoneNumber,
                        LocalDateTime.now(),
                        AuthCodeType.SIGNUP,
                        true);

                given(existingMember.getId()).willReturn(memberId);
                given(memberRepository.findByPhoneNumber(phoneNumber)).willReturn(Optional.of(existingMember));
                given(oneseoRepository.findByMember(existingMember)).willReturn(Optional.of(existingOneseo));
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, AuthCodeType.SIGNUP))
                        .willReturn(Optional.of(existingAuthCode));
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, AuthCodeType.TEST_RESULT))
                        .willReturn(Optional.empty());
            }

            @Test
            @DisplayName("회원과 관련된 모든 데이터를 삭제하고 memberId를 반환한다")
            void it_deletes_all_member_data_and_returns_member_id() {
                Long result = deleteMemberService.execute(phoneNumber);

                verify(oneseoRepository).delete(existingOneseo);
                verify(codeRepository).delete(existingAuthCode);
                verify(memberRepository).delete(existingMember);
                assertEquals(memberId, result);
            }
        }

        @Nested
        @DisplayName("Oneseo가 존재하지 않는 회원의 전화번호가 주어지면")
        class Context_with_member_without_oneseo {

            private Member existingMember;

            @BeforeEach
            void setUp() {
                existingMember = mock(Member.class);

                given(existingMember.getId()).willReturn(memberId);
                given(memberRepository.findByPhoneNumber(phoneNumber)).willReturn(Optional.of(existingMember));
                given(oneseoRepository.findByMember(existingMember)).willReturn(Optional.empty());

                for (AuthCodeType authCodeType : AuthCodeType.values()) {
                    given(codeRepository.findByMemberIdAndAuthCodeType(memberId, authCodeType))
                            .willReturn(Optional.empty());
                }
            }

            @Test
            @DisplayName("회원만 삭제하고 memberId를 반환한다")
            void it_deletes_only_member_and_returns_member_id() {
                Long result = deleteMemberService.execute(phoneNumber);

                verify(oneseoRepository, never()).delete(any(Oneseo.class));
                verify(codeRepository, never()).delete(any(AuthenticationCode.class));
                verify(memberRepository).delete(existingMember);
                assertEquals(memberId, result);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 전화번호가 주어지면")
        class Context_with_non_existing_phone_number {

            @BeforeEach
            void setUp() {
                given(memberRepository.findByPhoneNumber(phoneNumber)).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> deleteMemberService.execute(phoneNumber));

                assertEquals("해당 전화 번호에 해당하는 계정이 존재하지 않습니다.", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
