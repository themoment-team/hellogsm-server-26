package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 생활기록부에서 추출한 중학교 성적입니다.
 *
 * <p>
 * achievement 필드는 원서 작성 폼에 그대로 주입할 수 있는 형태이며, 서버는 이 결과를 저장하지 않습니다. 실제 저장은 기존 원서
 * 등록/수정 API를 통해 이루어집니다.
 */
@Builder
public record ExtractedAchievementResDto(
        @Schema(description = "추출된 성적. 폼에 그대로 주입 가능한 형태입니다.") MiddleSchoolAchievementResDto achievement,
        @Schema(description = "신뢰도 및 검수 필요 항목") ExtractionMetaResDto meta) {
}
