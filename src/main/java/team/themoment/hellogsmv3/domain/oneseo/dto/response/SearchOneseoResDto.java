package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Builder
public record SearchOneseoResDto(@Schema(description = "지원자 회원 ID") Long memberId,
        @Schema(description = "원서 접수번호") String submitCode,
        @Schema(description = "실물 원서 도착 여부 (YES, NO)") YesNo realOneseoArrivedYn,
        @Schema(description = "지원자 이름") String name, @Schema(description = "지원 전형") Screening screening,
        @Schema(description = "중학교 이름") String schoolName, @Schema(description = "지원자 전화번호") String phoneNumber,
        @Schema(description = "보호자 전화번호") String guardianPhoneNumber,
        @Schema(description = "담임교사 전화번호") String schoolTeacherPhoneNumber,
        @Schema(description = "수험번호. 발급 전에는 null", nullable = true) String examinationNumber,
        @Schema(description = "1차 합격 여부 (YES, NO). 결과 산출 전에는 null", nullable = true) YesNo firstTestPassYn,
        @Schema(description = "역량검사 점수. 입력 전에는 null", nullable = true) BigDecimal competencyEvaluationScore,
        @Schema(description = "심층면접 점수. 입력 전에는 null", nullable = true) BigDecimal interviewScore,
        @Schema(description = "2차 합격 여부 (YES, NO). 결과가 없으면 null", nullable = true) YesNo secondTestPassYn,
        @Schema(description = "입학등록 동의서 제출 여부. 미설정이면 null", nullable = true) YesNo entranceIntentionYn,
        @Schema(description = "해당 원서의 수정 상태 (NONE, REQUESTED, APPROVED)", nullable = true) OneseoEditStatus oneseoEditStatus) {
}
