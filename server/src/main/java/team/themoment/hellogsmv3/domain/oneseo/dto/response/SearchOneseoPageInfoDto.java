package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import lombok.Builder;

@Builder
public record SearchOneseoPageInfoDto(Integer totalPages, Long totalElements) {
}
