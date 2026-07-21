package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import jakarta.validation.constraints.NotNull;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public record EntranceIntentionReqDto(@NotNull YesNo entranceIntentionYn) {
}
