package team.themoment.hellogsmv3.domain.common.date.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import team.themoment.hellogsmv3.domain.common.date.dto.DateResDto;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryDateService 클래스의")
class QueryDateServiceTest {

    @Mock
    private ScheduleEnvironment scheduleEnv;

    @InjectMocks
    private QueryDateService queryDateService;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final LocalDateTime submissionStart = LocalDateTime.of(2025, 1, 1, 9, 0);
        private final LocalDateTime submissionEnd = LocalDateTime.of(2025, 1, 10, 18, 0);
        private final LocalDateTime firstResults = LocalDateTime.of(2025, 2, 1, 9, 0);
        private final LocalDateTime competency = LocalDateTime.of(2025, 2, 15, 9, 0);
        private final LocalDateTime interview = LocalDateTime.of(2025, 3, 1, 9, 0);
        private final LocalDateTime finalResults = LocalDateTime.of(2025, 3, 15, 9, 0);

        @Nested
        @DisplayName("ScheduleEnvironment가 정상적으로 주어지면")
        class Context_with_valid_schedule_environment {

            @BeforeEach
            void setUp() {
                given(scheduleEnv.oneseoSubmissionStart()).willReturn(submissionStart);
                given(scheduleEnv.oneseoSubmissionEnd()).willReturn(submissionEnd);
                given(scheduleEnv.firstResultsAnnouncement()).willReturn(firstResults);
                given(scheduleEnv.competencyEvaluation()).willReturn(competency);
                given(scheduleEnv.interview()).willReturn(interview);
                given(scheduleEnv.finalResultsAnnouncement()).willReturn(finalResults);
            }

            @Test
            @DisplayName("모든 일정 정보를 담은 DateResDto를 반환한다")
            void it_returns_date_res_dto_with_all_schedule_info() {
                DateResDto result = queryDateService.execute();

                assertEquals(submissionStart, result.oneseoSubmissionStart());
                assertEquals(submissionEnd, result.oneseoSubmissionEnd());
                assertEquals(firstResults, result.firstResultsAnnouncement());
                assertEquals(competency, result.competencyEvaluation());
                assertEquals(interview, result.inDepthInterview());
                assertEquals(finalResults, result.finalResultsAnnouncement());
            }
        }
    }
}
