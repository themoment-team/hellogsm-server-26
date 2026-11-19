package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;

@Builder
public record FoundOneseoResDto(@Schema(description = "원서 ID. 임시저장본에서는 null", nullable = true) Long oneseoId,
        @Schema(description = "원서 접수번호 . 임시저장본에서는 null", nullable = true) String submitCode,
        @Schema(description = "지원 전형") Screening wantedScreening,
        @Schema(description = "지망 학과 정보") DesiredMajorsResDto desiredMajors,
        @Schema(description = "지원자 인적사항. 임시저장본에서는 일부 값이 null일 수 있음") OneseoPrivacyDetailResDto privacyDetail,
        @Schema(description = "중학교 성적. GED·자유학기·미입력 임시저장 항목은 null일 수 있음") MiddleSchoolAchievementResDto middleSchoolAchievement,
        @Schema(description = "계산된 성적. 임시저장 입력이 불충분하면 생략", nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) CalculatedScoreResDto calculatedScore,
        @Schema(description = "임시저장 진행 단계. 임시저장본에만 포함하며 제출 원서 조회 시 생략", nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) Integer step) {
}
