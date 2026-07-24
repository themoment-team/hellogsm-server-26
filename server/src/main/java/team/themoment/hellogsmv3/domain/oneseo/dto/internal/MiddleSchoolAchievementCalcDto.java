package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;

/**
 * 중학교 성적 계산용 DTO — 원본 검증만 거친 상태입니다. 과목의 이름을 나타내는
 * generalSubjects,newSubjects,artsPhysicalSubjects가 없습니다.
 *
 * 결측 학기 대체(빈 학기를 다른 학기 성적으로 채우는 것)는 여기서 하지 않습니다 — 최종 채점 시점에 entrance-engine의
 * MissingSemesterStrategy가 plan 선언대로 적용합니다.
 */
@Builder
public record MiddleSchoolAchievementCalcDto(List<Integer> achievement1_1, List<Integer> achievement1_2,
        List<Integer> achievement2_1, List<Integer> achievement2_2, List<Integer> achievement3_1,
        List<Integer> achievement3_2, List<Integer> artsPhysicalAchievement, List<Integer> absentDays,
        List<Integer> attendanceDays, List<Integer> volunteerTime, String liberalSystem, String freeSemester,
        BigDecimal gedAvgScore) {
}
