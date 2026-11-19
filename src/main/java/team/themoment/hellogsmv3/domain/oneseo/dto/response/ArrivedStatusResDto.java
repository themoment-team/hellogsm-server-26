package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public record ArrivedStatusResDto(@Schema(description = "실물 원서 도착 여부 (YES, NO) . 토글 이후의 값") YesNo realOneseoArrivedYn) {
}
