package team.themoment.hellogsmv3.domain.common.operation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Getter
@Builder
@AllArgsConstructor
public class AnnounceTestResultResDto {
    private YesNo firstTestResultAnnouncementYn;
    private YesNo secondTestResultAnnouncementYn;
}
