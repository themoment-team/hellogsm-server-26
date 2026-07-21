package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;

/**
 * 중학교 성적 계산용 DTO 성적이 복사된 상태의 DTO입니다. 과목의 이름을 나타내는
 * generalSubjects,newSubjects,artsPhysicalSubjects가 없습니다.
 */
@Builder
public record MiddleSchoolAchievementCalcDto(List<Integer> achievement1_2, List<Integer> achievement2_1,
        List<Integer> achievement2_2, List<Integer> achievement3_1, List<Integer> achievement3_2,
        List<Integer> artsPhysicalAchievement, List<Integer> absentDays, List<Integer> attendanceDays,
        List<Integer> volunteerTime, String liberalSystem, String freeSemester, BigDecimal gedAvgScore) {
}
