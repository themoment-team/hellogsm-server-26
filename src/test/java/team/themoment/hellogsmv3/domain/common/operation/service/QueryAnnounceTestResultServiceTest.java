package team.themoment.hellogsmv3.domain.common.operation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.common.operation.dto.response.AnnounceTestResultResDto;
import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.common.operation.repository.OperationTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("QueryAnnounceTestResultService 클래스의")
public class QueryAnnounceTestResultServiceTest {

    @Mock
    private OperationTestResultRepository operationTestResultRepository;

    @InjectMocks
    private QueryAnnounceTestResultService queryAnnounceTestResultService;

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
                given(operationTestResultRepository.findTestResult()).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> queryAnnounceTestResultService.execute());

                assertEquals("시험 운영 정보를 찾을 수 없습니다.", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("시험 운영 정보가 있을 경우")
        class Context_operation_test_result_exists {

            OperationTestResult testResult;

            @BeforeEach
            void setup() {
                testResult = OperationTestResult.builder().firstTestResultAnnouncementYn(NO)
                        .secondTestResultAnnouncementYn(NO).build();

                given(operationTestResultRepository.findTestResult()).willReturn(Optional.of(testResult));
            }

            @Test
            @DisplayName("1차 테스트 결과 발표 여부와 2차 테스트 결과 발표 여부를 반환한다")
            void it_returns_first_and_second_test_result_announcement_yn() {
                AnnounceTestResultResDto result = queryAnnounceTestResultService.execute();

                assertEquals(testResult.getFirstTestResultAnnouncementYn(), result.getFirstTestResultAnnouncementYn());
                assertEquals(testResult.getSecondTestResultAnnouncementYn(),
                        result.getSecondTestResultAnnouncementYn());
            }
        }
    }
}
