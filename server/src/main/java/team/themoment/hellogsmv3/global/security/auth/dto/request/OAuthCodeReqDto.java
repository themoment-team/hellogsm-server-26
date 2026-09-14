package team.themoment.hellogsmv3.global.security.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthCodeReqDto(
        @Schema(description = "OAuth Provider로부터 받은 Authorization Code") @NotBlank(message = "Authorization Code는 필수입니다.") String code,
        @Schema(description = "OAuth 인증 후 리다이렉트될 URI") @NotBlank(message = "redirect_uri는 필수입니다.") String redirectUri) {
}
