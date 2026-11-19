package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.annotation.ValidSubjectName;

/** 중학교 성적 Request DTO Request 용도로만 사용되어야 하고, 아직 성적이 복사되지 않은 상태의 DTO입니다. */
@Builder
@ValidSubjectName
public record MiddleSchoolAchievementReqDto(
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "1학년 1학기 일반교과 성취점수. 값은 0~5, null 원소 불가.", nullable = true) List<Integer> achievement1_1,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "1학년 2학기 일반교과 성취점수. 값은 0~5, null 원소 불가.", nullable = true, example = "[4, 5, 3, 5, 4, 5, 3, 5, 2]") List<Integer> achievement1_2,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "2학년 1학기 일반교과 성취점수. 값은 0~5, null 원소 불가.", nullable = true, example = "[4, 5, 3, 5, 4, 5, 3, 5, 2]") List<Integer> achievement2_1,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "2학년 2학기 일반교과 성취점수. 값은 0~5, null 원소 불가.", nullable = true, example = "[5, 2, 5, 5, 4, 1, 5, 5, 0]") List<Integer> achievement2_2,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "3학년 1학기 일반교과 성취점수. 값은 0~5, null 원소 불가.", nullable = true, example = "[3, 5, 3, 5, 1, 3, 5, 2, 0]") List<Integer> achievement3_1,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "3학년 2학기 일반교과 성취점수. 값은 0~5, null 원소 불가.", nullable = true) List<Integer> achievement3_2,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "일반교과 과목 이름. 세 과목 목록이 모두 주어지면 서로 겹치는 과목명은 허용하지 않습니다.", nullable = true, example = "[\"국어\", \"도덕\", \"사회\", \"역사\", \"수학\", \"과학\", \"기술가정\", \"영어\"]") List<String> generalSubjects,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "일반교과 추가 과목 이름. 세 과목 목록이 모두 주어지면 서로 겹치는 과목명은 허용하지 않습니다.", nullable = true, example = "[\"프로그래밍\"]") List<String> newSubjects,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "예체능 교과 성취점수. 0 또는 3~5, null 원소 불가. 자유학기제·자유학년제인 경우 학기 수 × 3개. CANDIDATE는 자유학기제 5학기·자유학년제 3학기, GRADUATE는 각각 6학기·4학기. 예체능 과목이 있으면 필수.", nullable = true, example = "[5, 4, 5, 3, 5, 0, 5, 3, 5]") List<Integer> artsPhysicalAchievement,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "예체능 교과 이름. 세 과목 목록이 모두 주어지면 서로 겹치는 과목명은 허용하지 않습니다.", nullable = true, example = "[\"체육\", \"미술\", \"음악\"]") List<String> artsPhysicalSubjects,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "결석일. 출결 계산 시 null 원소 불가.", nullable = true, example = "[2, 0, 0]") List<Integer> absentDays,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "null 원소 불가. 합계의 1/3(정수 나눗셈)이 결석일에 가산됩니다. 지각·조퇴·결과 횟수", nullable = true, example = "[0, 4, 0, 0, 0, 0, 0, 0, 0]") List<Integer> attendanceDays,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "봉사시간", nullable = true, example = "[7, 3, 4]") List<@PositiveOrZero(message = "봉사시간은 0 이상이어야 합니다.") Integer> volunteerTime,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "자유학기 or 학년제 시스템. 권장 입력은 자유학기제·자유학년제이며 이 API에서는 문자열 자체를 검증하지 않습니다.", nullable = true, example = "자유학년제") String liberalSystem,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "자유학기제 학기. CANDIDATE이면서 자유학기제이면 필수.", nullable = true) String freeSemester,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "검정고시 전과목 득점 평균. GED 성적 계산에 필요한 평균 점수. GED에서는 다른 성적 대신 이 값을 사용합니다.", nullable = true) BigDecimal gedAvgScore) {
}
