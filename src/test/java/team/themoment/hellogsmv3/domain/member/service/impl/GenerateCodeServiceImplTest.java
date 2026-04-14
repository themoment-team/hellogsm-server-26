package team.themoment.hellogsmv3.domain.member.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.dto.request.GenerateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.domain.member.service.SendCodeNotificationService;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("GenerateTestResultCodeServiceImpl 클래스의")
class GenerateCodeServiceImplTest {

    @Mock
    private CodeRepository codeRepository;

    @Mock
    private SendCodeNotificationService sendCodeNotificationService;

    @InjectMocks
    private GenerateCodeServiceImpl generateCodeServiceImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final GenerateCodeReqDto reqDto = new GenerateCodeReqDto("01012345678");

        @Nested
        @DisplayName("유효한 요청이 주어지면")
        class Context_with_valid_request {

            @BeforeEach
            void setUp() {
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP)).willReturn(Optional.empty());
                given(codeRepository.findByCode(anyString())).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("새로운 코드를 생성하고 저장 후 인증번호를 전송한다")
            void it_generates_and_saves_a_new_code() {
                String code = generateCodeServiceImpl.execute(memberId, reqDto);

                assertNotNull(code);
                assertEquals(6, code.length());

                ArgumentCaptor<AuthenticationCode> authCodeCaptor = ArgumentCaptor.forClass(AuthenticationCode.class);
                verify(codeRepository).save(authCodeCaptor.capture());
                AuthenticationCode savedAuthCode = authCodeCaptor.getValue();

                verify(sendCodeNotificationService).execute(reqDto.phoneNumber(), code);

                assertEquals(memberId, savedAuthCode.getMemberId());
                assertEquals(reqDto.phoneNumber(), savedAuthCode.getPhoneNumber());
                assertEquals(code, savedAuthCode.getCode());
                assertEquals(1, savedAuthCode.getCount());
            }
        }

        @Nested
        @DisplayName("요청 제한 횟수를 초과하면")
        class Context_with_exceeded_request_limit {

            @Mock
            private AuthenticationCode existingCode;

            @BeforeEach
            void setUp() {
                existingCode = mock(AuthenticationCode.class);
                given(codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP))
                        .willReturn(Optional.of(existingCode));
                given(existingCode.getCount()).willReturn(5);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_an_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> generateCodeServiceImpl.execute(memberId, reqDto));
                assertEquals("너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요. 특정 시간 내 제한 횟수인 5회를 초과하였습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }
}
