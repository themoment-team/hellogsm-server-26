package team.themoment.hellogsmv3.domain.common.operation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

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

@DisplayName("AnnounceFirstTestResultService 클래스의")
class AnnounceFirstTestResultServiceTest {

    @Mock
    private OperationTestResultRepository operationTestResultRepository;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;
    @Mock
    private ScheduleEnvironment scheduleEnv;

    @InjectMocks
    private AnnounceFirstTestResultService announceFirstTestResultService;

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
                given(scheduleEnv.firstResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceFirstTestResultService.execute());

                assertEquals("시험 운영 정보를 찾을 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 결과 발표 기간 이전일 경우")
        class Context_before_first_test_result_announcement {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.firstResultsAnnouncement()).willReturn(LocalDateTime.now().plusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceFirstTestResultService.execute());

                assertEquals("1차 결과 발표 기간 이전에 발표 여부를 수정할 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("1차 결과가 이미 발표된 경우")
        class Context_first_test_already_announced {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.firstResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);

                OperationTestResult testResult = mock(OperationTestResult.class);

                given(testResult.getFirstTestResultAnnouncementYn()).willReturn(YES);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> announceFirstTestResultService.execute());

                assertEquals("이미 1차 결과를 발표했습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("정상 조건이라면")
        class Context_valid_condition {

            OperationTestResult testResult = mock(OperationTestResult.class);

            @BeforeEach
            void setUp() {
                given(scheduleEnv.firstResultsAnnouncement()).willReturn(LocalDateTime.now().minusDays(1));
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNull()).willReturn(false);
                given(testResult.getFirstTestResultAnnouncementYn()).willReturn(NO);
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("1차 결과를 발표하고 저장한다")
            void it_announces_and_saves() {
                announceFirstTestResultService.execute();
                verify(testResult).announceFirstTestResult();
                verify(operationTestResultRepository).save(testResult);
            }
        }
    }
}
