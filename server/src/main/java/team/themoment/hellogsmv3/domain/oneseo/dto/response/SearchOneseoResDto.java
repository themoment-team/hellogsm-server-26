package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Builder
public record SearchOneseoResDto(Long memberId, String submitCode, YesNo realOneseoArrivedYn, String name,
        Screening screening, String schoolName, String phoneNumber, String guardianPhoneNumber,
        String schoolTeacherPhoneNumber, String examinationNumber, YesNo firstTestPassYn,
        BigDecimal competencyEvaluationScore, BigDecimal interviewScore, YesNo secondTestPassYn,
        YesNo entranceIntentionYn, OneseoEditStatus oneseoEditStatus) {
}
