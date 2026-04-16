package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("ModifyOneseoByApplicantService 클래스의")
class ModifyOneseoByApplicantServiceTest {

    @Mock
    private OneseoService oneseoService;

    @Mock
    private ModifyOneseoService modifyOneseoService;

    @InjectMocks
    private ModifyOneseoByApplicantService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private OneseoReqDto reqDto;
        private Oneseo oneseo;

        @BeforeEach
        void setUp() {
            reqDto = mock(OneseoReqDto.class);
            oneseo = mock(Oneseo.class);
            given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
        }

        @Nested
        @DisplayName("APPROVED 상태가 아닌 경우")
        class Context_when_not_approved {

            @Test
            @DisplayName("NONE 상태이면 FORBIDDEN ExpectedException을 던진다")
            void it_throws_forbidden_when_none() {
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.NONE);

                ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(reqDto, memberId));
                assert ex.getStatusCode() == HttpStatus.FORBIDDEN;
            }

            @Test
            @DisplayName("REQUESTED 상태이면 FORBIDDEN ExpectedException을 던진다")
            void it_throws_forbidden_when_requested() {
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.REQUESTED);

                ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(reqDto, memberId));
                assert ex.getStatusCode() == HttpStatus.FORBIDDEN;
            }
        }

        @Nested
        @DisplayName("APPROVED 상태인 경우")
        class Context_when_approved {

            @BeforeEach
            void setUp() {
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.APPROVED);
            }

            @Test
            @DisplayName("modifyOneseoService.execute를 호출한다")
            void it_calls_modify_oneseo_service() {
                assertDoesNotThrow(() -> service.execute(reqDto, memberId));
                verify(modifyOneseoService).execute(reqDto, memberId);
            }
        }
    }
}
