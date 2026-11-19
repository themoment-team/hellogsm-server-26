package team.themoment.hellogsmv3.domain.common.operation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Getter
@Builder
@AllArgsConstructor
public class AnnounceTestResultResDto {
    @Schema(description = "1차 결과 발표 여부", example = "NO", allowableValues = {"YES", "NO"})
    private YesNo firstTestResultAnnouncementYn;
    @Schema(description = "2차 결과 발표 여부", example = "NO", allowableValues = {"YES", "NO"})
    private YesNo secondTestResultAnnouncementYn;
}
