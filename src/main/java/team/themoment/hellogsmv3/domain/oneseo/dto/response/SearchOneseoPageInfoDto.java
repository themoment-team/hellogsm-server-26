package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SearchOneseoPageInfoDto(@Schema(description = "전체 페이지 수") Integer totalPages,
        @Schema(description = "전체 검색 결과 수") Long totalElements) {
}
