package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeneralSubjectsScoreDetailResDto(BigDecimal score1_2, BigDecimal score2_1, BigDecimal score2_2,
        BigDecimal score3_1, BigDecimal score3_2) {
}
