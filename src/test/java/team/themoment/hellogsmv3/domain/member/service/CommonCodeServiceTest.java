package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;

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
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("CommonCodeService 클래스의")
class CommonCodeServiceTest {

    @Mock
    private CodeRepository codeRepository;

    @InjectMocks
    private CommonCodeService commonCodeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("validateAndDelete 메소드는")
    class Describe_validateAndDelete {

        private final Long memberId = 1L;
        private final String validCode = "validCode";
        private final String validPhoneNumber = "01012345678";
        private final String invalidCode = "invalidCode";
        private final String invalidPhoneNumber = "01087654321";
        private AuthenticationCode authenticationCode;

        @BeforeEach
        void setUp() {
            authenticationCode = new AuthenticationCode(memberId,
                    validCode,
                    validPhoneNumber,
                    LocalDateTime.now(),
                    SIGNUP,
                    false);
        }

        @Nested
        @DisplayName("유효한 회원 ID와 코드와 전화번호가 주어지면")
        class Context_with_valid_code_and_phone_number {

            @BeforeEach
            void setUp() {
                authenticationCode.authenticatedAuthenticationCode();
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(authenticationCode));
            }

            @Test
            @DisplayName("코드를 삭제한다")
            void it_deletes_the_code() {
                commonCodeService.validateAndDelete(memberId, validCode, validPhoneNumber, SIGNUP);

                verify(codeRepository).delete(authenticationCode);
            }
        }

        @Nested
        @DisplayName("인증 코드가 존재하지 않으면")
        class Context_with_non_existing_code {

            @BeforeEach
            void setUp() {
                given(codeRepository.findByMemberIdAndAuthCodeType(anyLong(), eq(SIGNUP))).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    commonCodeService.validateAndDelete(memberId, validCode, validPhoneNumber, SIGNUP);
                });

                assertEquals("사용자의 code가 존재하지 않습니다. 사용자의 ID : " + memberId, exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("인증코드가 인증받지 않았다면")
        class Context_with_unauthenticated_code {

            @BeforeEach
            void setUp() {
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(authenticationCode));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception_when_code_is_not_authenticated() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    commonCodeService.validateAndDelete(memberId, validCode, validPhoneNumber, SIGNUP);
                });

                assertEquals("유효하지 않은 요청입니다. 인증받지 않은 code입니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("잘못된 코드가 주어지면")
        class Context_with_invalid_code {

            @BeforeEach
            void setUp() {
                authenticationCode.authenticatedAuthenticationCode();
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(authenticationCode));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception_when_code_is_invalid() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    commonCodeService.validateAndDelete(memberId, invalidCode, validPhoneNumber, SIGNUP);
                });

                assertEquals("유효하지 않은 요청입니다. 이전 혹은 잘못된 형식의 code입니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("잘못된 전화번호가 주어지면")
        class Context_with_invalid_phone_number {

            @BeforeEach
            void setUp() {
                authenticationCode.authenticatedAuthenticationCode();
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(authenticationCode));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    commonCodeService.validateAndDelete(memberId, validCode, invalidPhoneNumber, SIGNUP);
                });

                assertEquals("유효하지 않은 요청입니다. code인증에 사용되었던 전화번호와 요청에 사용한 전화번호가 일치하지 않습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }
}
