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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import team.themoment.hellogsmv3.domain.member.dto.request.GenerateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateTestCodeServiceImpl 클래스의")
class GenerateTestCodeServiceImplTest {

    @Mock
    private CodeRepository codeRepository;

    @InjectMocks
    private GenerateTestCodeServiceImpl generateTestCodeServiceImpl;

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
            @DisplayName("새로운 테스트 코드를 생성하고 저장한다")
            void it_generates_and_saves_a_new_test_code() {
                String code = generateTestCodeServiceImpl.execute(memberId, reqDto);

                assertNotNull(code);
                assertEquals(6, code.length());

                ArgumentCaptor<AuthenticationCode> authCodeCaptor = ArgumentCaptor.forClass(AuthenticationCode.class);
                verify(codeRepository).save(authCodeCaptor.capture());
                AuthenticationCode savedAuthCode = authCodeCaptor.getValue();

                assertEquals(memberId, savedAuthCode.getMemberId());
                assertEquals(reqDto.phoneNumber(), savedAuthCode.getPhoneNumber());
                assertEquals(code, savedAuthCode.getCode());
                assertEquals(0, savedAuthCode.getCount());
            }
        }
    }
}
