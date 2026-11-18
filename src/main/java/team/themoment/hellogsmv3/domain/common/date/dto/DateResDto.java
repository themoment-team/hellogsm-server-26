package team.themoment.hellogsmv3.domain.common.date.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record DateResDto(
        @Schema(description = "원서 접수 시작 일정. KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙이며 UTC가 아닙니다.", implementation = String.class, type = "string", format = "", pattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", example = "2027-01-01T09:00:00Z") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime oneseoSubmissionStart,
        @Schema(description = "원서 접수 종료 일정. KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙이며 UTC가 아닙니다.", implementation = String.class, type = "string", format = "", pattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", example = "2027-01-01T09:00:00Z") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime oneseoSubmissionEnd,
        @Schema(description = "1차 결과 발표 일정. KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙이며 UTC가 아닙니다.", implementation = String.class, type = "string", format = "", pattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", example = "2027-01-01T09:00:00Z") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime firstResultsAnnouncement,
        @Schema(description = "직무 적성 소양 평가 일정. KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙이며 UTC가 아닙니다.", implementation = String.class, type = "string", format = "", pattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", example = "2027-01-01T09:00:00Z") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime competencyEvaluation,
        @Schema(description = "심층면접(schedule.interview) 일정. KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙이며 UTC가 아닙니다.", implementation = String.class, type = "string", format = "", pattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", example = "2027-01-01T09:00:00Z") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime inDepthInterview,
        @Schema(description = "최종 결과 발표 일정. KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙이며 UTC가 아닙니다.", implementation = String.class, type = "string", format = "", pattern = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", example = "2027-01-01T09:00:00Z") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") LocalDateTime finalResultsAnnouncement) {
}
