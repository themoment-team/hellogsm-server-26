package team.themoment.hellogsmv3.domain.member.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

@Builder
public record FoundMemberResDto(Long memberId, String name, String phoneNumber, LocalDate birth, Sex sex) {
}
