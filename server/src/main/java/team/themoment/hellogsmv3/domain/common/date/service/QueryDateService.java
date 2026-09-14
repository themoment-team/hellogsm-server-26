package team.themoment.hellogsmv3.domain.common.date.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.date.dto.DateResDto;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

@Service
@RequiredArgsConstructor
public class QueryDateService {

    private final ScheduleEnvironment scheduleEnv;

    public DateResDto execute() {
        return DateResDto.builder().oneseoSubmissionStart(scheduleEnv.oneseoSubmissionStart())
                .oneseoSubmissionEnd(scheduleEnv.oneseoSubmissionEnd())
                .firstResultsAnnouncement(scheduleEnv.firstResultsAnnouncement())
                .competencyEvaluation(scheduleEnv.competencyEvaluation()).inDepthInterview(scheduleEnv.interview())
                .finalResultsAnnouncement(scheduleEnv.finalResultsAnnouncement()).build();
    }
}
