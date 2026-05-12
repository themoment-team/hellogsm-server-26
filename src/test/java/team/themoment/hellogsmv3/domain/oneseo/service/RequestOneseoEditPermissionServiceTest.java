package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestOneseoEditPermissionService 클래스의")
class RequestOneseoEditPermissionServiceTest {

    @Mock
    private OneseoService oneseoService;

    private RequestOneseoEditPermissionService service;

    @BeforeEach
    void setUp() {
        service = spy(new RequestOneseoEditPermissionService(oneseoService));
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;

        @Nested
        @DisplayName("현재 시간이 09:00 ~ 16:00 범위 밖인 경우")
        class Context_when_outside_allowed_time {

            @Test
            @DisplayName("FORBIDDEN ExpectedException을 던진다")
            void it_throws_forbidden() {
                given(service.getCurrentTime()).willReturn(LocalTime.of(8, 59));

                ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(memberId));
                assert ex.getStatusCode() == HttpStatus.FORBIDDEN;
            }
        }

        @Nested
        @DisplayName("현재 시간이 09:00 ~ 16:00 범위 내인 경우")
        class Context_when_within_allowed_time {

            private Oneseo oneseo;
            private EntranceTestResult entranceTestResult;

            @BeforeEach
            void setUp() {
                entranceTestResult = mock(EntranceTestResult.class);
                oneseo = mock(Oneseo.class);
                given(oneseo.getEntranceTestResult()).willReturn(entranceTestResult);
                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(service.getCurrentTime()).willReturn(LocalTime.of(10, 0));
            }

            @Nested
            @DisplayName("1차 전형이 이미 완료된 경우")
            class Context_when_first_test_finished {

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    given(entranceTestResult.getFirstTestPassYn()).willReturn(YesNo.YES);

                    try (MockedStatic<OneseoService> mockedOneseoService = mockStatic(OneseoService.class)) {
                        mockedOneseoService.when(() -> OneseoService.isBeforeFirstTest(YesNo.YES))
                                .thenThrow(new ExpectedException(HttpStatus.FORBIDDEN));

                        assertThrows(ExpectedException.class, () -> service.execute(memberId));
                    }
                }
            }

            @Nested
            @DisplayName("이미 APPROVED 상태인 경우")
            class Context_when_already_approved {

                @Test
                @DisplayName("CONFLICT ExpectedException을 던진다")
                void it_throws_conflict() {
                    given(entranceTestResult.getFirstTestPassYn()).willReturn(null);
                    given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.APPROVED);

                    ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(memberId));
                    assert ex.getStatusCode() == HttpStatus.CONFLICT;
                }
            }

            @Nested
            @DisplayName("이미 REQUESTED 상태인 경우")
            class Context_when_already_requested {

                @Test
                @DisplayName("CONFLICT ExpectedException을 던진다")
                void it_throws_conflict() {
                    given(entranceTestResult.getFirstTestPassYn()).willReturn(null);
                    given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.REQUESTED);

                    ExpectedException ex = assertThrows(ExpectedException.class, () -> service.execute(memberId));
                    assert ex.getStatusCode() == HttpStatus.CONFLICT;
                }
            }

            @Nested
            @DisplayName("NONE 상태이고 정상 조건인 경우")
            class Context_when_none_status_and_valid {

                @Test
                @DisplayName("requestEditPermit을 호출한다")
                void it_calls_request_edit_permit() {
                    given(entranceTestResult.getFirstTestPassYn()).willReturn(null);
                    given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.NONE);

                    assertDoesNotThrow(() -> service.execute(memberId));
                    verify(oneseo).requestEditPermit();
                }
            }
        }
    }
}
