package team.themoment.hellogsmv3.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AuthenticateCodeReqDto(
        @NotBlank @Schema(description = "인증 완료할 가입 인증코드", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED) String code) {
}
