package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MiddleSchoolAchievementResDto(
        @Schema(description = "1학년 1학기 일반교과 성취점수. 자유학기·GED·미입력 임시저장 값은 null일 수 있음", nullable = true) List<Integer> achievement1_1,
        @Schema(description = "1학년 2학기 일반교과 성취점수. 자유학기·GED·미입력 임시저장 값은 null일 수 있음", nullable = true) List<Integer> achievement1_2,
        @Schema(description = "2학년 1학기 일반교과 성취점수. 자유학기·GED·미입력 임시저장 값은 null일 수 있음", nullable = true) List<Integer> achievement2_1,
        @Schema(description = "2학년 2학기 일반교과 성취점수. 자유학기·GED·미입력 임시저장 값은 null일 수 있음", nullable = true) List<Integer> achievement2_2,
        @Schema(description = "3학년 1학기 일반교과 성취점수. 자유학기·GED·미입력 임시저장 값은 null일 수 있음", nullable = true) List<Integer> achievement3_1,
        @Schema(description = "3학년 2학기 일반교과 성취점수. 자유학기·GED·미입력 임시저장 값은 null일 수 있음", nullable = true) List<Integer> achievement3_2,
        @Schema(description = "일반교과 과목 목록", nullable = true) List<String> generalSubjects,
        @Schema(description = "추가 일반교과 과목 목록", nullable = true) List<String> newSubjects,
        @Schema(description = "예체능 성취점수 목록", nullable = true) List<Integer> artsPhysicalAchievement,
        @Schema(description = "예체능 과목 목록", nullable = true) List<String> artsPhysicalSubjects,
        @Schema(description = "학년별 결석일", nullable = true) List<Integer> absentDays,
        @Schema(description = "결석일 합계 + 지각·조퇴·결과 합계의 1/3(정수 나눗셈)", nullable = true) Integer absentDaysCount,
        @Schema(description = "지각·조퇴·결과 횟수", nullable = true) List<Integer> attendanceDays,
        @Schema(description = "학년별 봉사시간", nullable = true) List<Integer> volunteerTime,
        @Schema(description = "자유학기제 또는 자유학년제", nullable = true) String liberalSystem,
        @Schema(description = "자유학기 학기", nullable = true) String freeSemester,
        @Schema(description = "검정고시 평균 점수", nullable = true) BigDecimal gedAvgScore) {
}
