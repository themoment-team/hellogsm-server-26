package team.themoment.hellogsmv3.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GenerateCodeReqDto(
        @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$") @NotNull String phoneNumber) {
}
