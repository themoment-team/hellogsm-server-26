package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;

@Builder
public record FoundOneseoResDto(Long oneseoId, String submitCode, Screening wantedScreening,
        DesiredMajorsResDto desiredMajors, OneseoPrivacyDetailResDto privacyDetail,
        MiddleSchoolAchievementResDto middleSchoolAchievement,
        @JsonInclude(JsonInclude.Include.NON_NULL) CalculatedScoreResDto calculatedScore,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer step) {
}
