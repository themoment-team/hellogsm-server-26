package team.themoment.hellogsmv3.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticateCodeReqDto(@NotBlank String code) {
}
