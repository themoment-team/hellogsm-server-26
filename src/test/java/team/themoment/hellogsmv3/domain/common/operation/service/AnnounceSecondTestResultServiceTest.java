package team.themoment.hellogsmv3.domain.common.operation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.YES;

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

import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.common.operation.repository.OperationTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

@DisplayName("AnnounceSecondTestResultService 클래스의")
class AnnounceSecondTestResultServiceTest {

    @Mock
    private OperationTestResultRepository operationTestResultRepository;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;
    @Mock
    private ScheduleEnvironment scheduleEnv;

    @InjectMocks
    private AnnounceSecondTestResultService announceSecondTestResultService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("시험 운영 정보가 없을 경우")
        class Context_operation_test_result_not_found {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(false);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("시험 운영 정보를 찾을 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("2차 결과 발표 기간 이전일 경우")
        class Context_before_second_test_result_announcement {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().plusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(false);

                OperationTestResult testResult = mock(OperationTestResult.class);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("2차 결과 발표 기간 이전에 발표 여부를 수정할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("2차 결과가 이미 발표된 경우")
        class Context_second_test_already_announced {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(false);

                OperationTestResult testResult = mock(OperationTestResult.class);

                given(testResult.getSecondTestResultAnnouncementYn()).willReturn(YES);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("이미 2차 결과를 발표했습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 배치가 진행되지 않은 경우")
        class Context_first_test_batch_not_processed {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(true);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(false);

                OperationTestResult testResult = mock(OperationTestResult.class);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("2차 결과 발표 기간 이전에 발표 여부를 수정할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 결과가 발표되지 않은 경우")
        class Context_first_test_result_not_announced {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(true);

                OperationTestResult testResult = mock(OperationTestResult.class);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("2차 결과 발표 기간 이전에 발표 여부를 수정할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 합격자 중 2차 결과가 입력되지 않은 경우")
        class Context_second_test_result_not_entered_for_first_pass {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(true);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(false);

                OperationTestResult testResult = mock(OperationTestResult.class);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("2차 결과 발표 기간 이전에 발표 여부를 수정할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 결과 발표 여부가 NO인 경우")
        class Context_first_test_result_announcement_is_no {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(true);

                OperationTestResult testResult = mock(OperationTestResult.class);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceSecondTestResultService.execute());

                assertEquals("2차 결과 발표 기간 이전에 발표 여부를 수정할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("2차 결과 발표 기간 이후이고, 아직 2차 결과가 발표되지 않은 경우")
        class Context_valid_condition {

            OperationTestResult testResult = mock(OperationTestResult.class);

            @BeforeEach
            void setUp() {
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(entranceTestResultRepository.existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES))
                        .willReturn(false);
                given(operationTestResultRepository.existsByFirstTestResultAnnouncementYn(NO)).willReturn(false);
                given(testResult.getSecondTestResultAnnouncementYn()).willReturn(NO);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("2차 결과를 발표하고 저장한다.")
            void it_announces_and_saves() {
                announceSecondTestResultService.execute();
                verify(testResult).announceSecondTestResult();
                verify(operationTestResultRepository).save(testResult);
            }
        }
    }
}
