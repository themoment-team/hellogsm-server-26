package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SearchOneseosResDto(@Schema(description = "검색 페이지 정보") SearchOneseoPageInfoDto info,
        @Schema(description = "현재 페이지의 원서 목록") List<SearchOneseoResDto> oneseos) {
}
