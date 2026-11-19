package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;

@Builder
public record DesiredMajorsResDto(@Schema(description = "1지망 학과 (SW, AI, IOT)") Major firstDesiredMajor,
        @Schema(description = "2지망 학과 (SW, AI, IOT)") Major secondDesiredMajor,
        @Schema(description = "3지망 학과 (SW, AI, IOT)") Major thirdDesiredMajor) {
}
