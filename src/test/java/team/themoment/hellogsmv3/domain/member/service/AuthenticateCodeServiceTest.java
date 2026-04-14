package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.dto.request.AuthenticateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("AuthenticateCodeService 클래스의")
public class AuthenticateCodeServiceTest {

    @Mock
    private CodeRepository codeRepository;

    @InjectMocks
    private AuthenticateCodeService authenticateCodeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final String validCode = "000000";
        private final String invalidCode = "111111";
        private final AuthenticateCodeReqDto reqDto = new AuthenticateCodeReqDto(validCode);
        private AuthenticationCode authenticationCode;

        @Nested
        @DisplayName("존재하는 인증 코드와 올바른 인증 코드가 주어지면")
        class Context_with_existing_code_and_valid_code {

            @BeforeEach
            void setUp() {
                authenticationCode = new AuthenticationCode(memberId,
                        validCode,
                        "01000000000",
                        LocalDateTime.now(),
                        SIGNUP,
                        false);
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(authenticationCode));
            }

            @Test
            @DisplayName("인증 코드를 인증 상태로 변경하고 저장한다.")
            void it_authenticates_code_and_saves() {
                authenticateCodeService.execute(memberId, reqDto, SIGNUP);

                ArgumentCaptor<AuthenticationCode> codeCaptor = ArgumentCaptor.forClass(AuthenticationCode.class);
                verify(codeRepository).save(codeCaptor.capture());

                AuthenticationCode capturedCode = codeCaptor.getValue();
                assertEquals(true, capturedCode.getAuthenticated());
            }
        }

        @Nested
        @DisplayName("존재하지 않는 인증 코드가 주어지면")
        class Context_with_non_existing_code {

            @BeforeEach
            void setUp() {
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP)).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> authenticateCodeService.execute(memberId, reqDto, SIGNUP));

                assertEquals("사용자의 code가 존재하지 않습니다. member ID : " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("잘못된 인증 코드가 주어지면")
        class Context_with_invalid_code {

            @BeforeEach
            void setUp() {
                authenticationCode = new AuthenticationCode(memberId,
                        validCode,
                        "01000000000",
                        LocalDateTime.now(),
                        SIGNUP,
                        false);
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(authenticationCode));
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                AuthenticateCodeReqDto invalidReqDto = new AuthenticateCodeReqDto(invalidCode);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> authenticateCodeService.execute(memberId, invalidReqDto, SIGNUP));

                assertEquals("유효하지 않은 code 이거나 이전 혹은 잘못된 code 입니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }
}
