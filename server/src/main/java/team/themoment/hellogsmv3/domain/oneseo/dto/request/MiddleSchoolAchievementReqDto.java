package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.annotation.ValidSubjectName;

/** 중학교 성적 Request DTO Request 용도로만 사용되어야 하고, 아직 성적이 복사되지 않은 상태의 DTO입니다. */
@Builder
@ValidSubjectName
public record MiddleSchoolAchievementReqDto(
        @Schema(description = "1학년 1학기 일반교과 성취점수", nullable = true, defaultValue = "null") List<Integer> achievement1_1,
        @Schema(description = "1학년 2학기 일반교과 성취점수", nullable = true, defaultValue = "[4, 5, 3, 5, 4, 5, 3, 5, 2]") List<Integer> achievement1_2,
        @Schema(description = "2학년 1학기 일반교과 성취점수", nullable = true, defaultValue = "[4, 5, 3, 5, 4, 5, 3, 5, 2]") List<Integer> achievement2_1,
        @Schema(description = "2학년 2학기 일반교과 성취점수", nullable = true, defaultValue = "[5, 2, 5, 5, 4, 1, 5, 5, 0]") List<Integer> achievement2_2,
        @Schema(description = "3학년 1학기 일반교과 성취점수", nullable = true, defaultValue = "[3, 5, 3, 5, 1, 3, 5, 2, 0]") List<Integer> achievement3_1,
        @Schema(description = "3학년 2학기 일반교과 성취점수", nullable = true, defaultValue = "null") List<Integer> achievement3_2,
        @Schema(description = "일반교과 과목 이름", nullable = true, defaultValue = "[\"국어\", \"도덕\", \"사회\", \"역사\", \"수학\", \"과학\", \"기술가정\", \"영어\"]") List<String> generalSubjects,
        @Schema(description = "일반교과 추가 과목 이름", nullable = true, defaultValue = "[\"프로그래밍\"]") List<String> newSubjects,
        @Schema(description = "예체능 교과 성취점수", nullable = true, defaultValue = "[5, 4, 5, 3, 5, 0, 5, 3, 5]") List<Integer> artsPhysicalAchievement,
        @Schema(description = "예체능 교과 이름", nullable = true, defaultValue = "[\"체육\", \"미술\", \"음악\"]") List<String> artsPhysicalSubjects,
        @Schema(description = "결석일", nullable = true, defaultValue = "[2, 0, 0]") List<Integer> absentDays,
        @Schema(description = "출결일", nullable = true, defaultValue = "[0, 4, 0, 0, 0, 0, 0, 0, 0]") List<Integer> attendanceDays,
        @Schema(description = "봉사시간", nullable = true, defaultValue = "[7, 3, 4]") List<Integer> volunteerTime,
        @Schema(description = "자유학기 or 학년제 시스템", nullable = true, defaultValue = "자유학년제", allowableValues = {
                "자유학기제", "자유학년제"}) String liberalSystem,
        @Schema(description = "자유학기제 학기", nullable = true, defaultValue = "null", allowableValues = {"1-1", "1-2",
                "2-1", "2-2", "3-1", "3-2"}) String freeSemester,
        @Schema(description = "검정고시 전과목 득점 평균", nullable = true, defaultValue = "null") BigDecimal gedAvgScore){
}
