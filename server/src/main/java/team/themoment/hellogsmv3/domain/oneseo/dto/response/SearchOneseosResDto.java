package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record SearchOneseosResDto(SearchOneseoPageInfoDto info, List<SearchOneseoResDto> oneseos) {
}
