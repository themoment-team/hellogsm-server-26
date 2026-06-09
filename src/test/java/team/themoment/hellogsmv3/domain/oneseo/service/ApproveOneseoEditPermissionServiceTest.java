package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApproveOneseoEditPermissionService 클래스의")
class ApproveOneseoEditPermissionServiceTest {

    @Mock
    private OneseoService oneseoService;

    @InjectMocks
    private ApproveOneseoEditPermissionService service;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private Oneseo oneseo;

        @BeforeEach
        void setUp() {
            oneseo = mock(Oneseo.class);
            given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
        }

        @Nested
        @DisplayName("REQUESTED 상태가 아닌 경우")
        class Context_when_not_requested {

            @Test
            @DisplayName("NONE 상태이면 BAD_REQUEST ExpectedException을 던진다")
            void it_throws_bad_request_when_none() {
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.NONE);

                ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(memberId));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertEquals("원서 수정 권한 요청이 존재하지 않습니다.", ex.getMessage());
            }

            @Test
            @DisplayName("APPROVED 상태이면 BAD_REQUEST ExpectedException을 던진다")
            void it_throws_bad_request_when_approved() {
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.APPROVED);

                ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(memberId));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertEquals("원서 수정 권한 요청이 존재하지 않습니다.", ex.getMessage());
            }
        }

        @Nested
        @DisplayName("REQUESTED 상태인 경우")
        class Context_when_requested {

            @BeforeEach
            void setUp() {
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.REQUESTED);
            }

            @Test
            @DisplayName("approveEditPermit을 호출한다")
            void it_calls_approve_edit_permit() {
                assertDoesNotThrow(() -> service.execute(memberId));
                verify(oneseo).approveEditPermit();
            }
        }
    }
}
