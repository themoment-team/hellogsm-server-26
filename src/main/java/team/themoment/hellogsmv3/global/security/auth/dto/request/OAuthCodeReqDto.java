package team.themoment.hellogsmv3.global.security.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthCodeReqDto(
        @Schema(description = "OAuth 제공자가 발급한 Authorization Code. 서버에서 URL 디코딩을 한 번 수행합니다.", requiredMode = Schema.RequiredMode.REQUIRED, example = "authorization-code") @NotBlank(message = "Authorization Code는 필수입니다.") String code,
        @Schema(description = "프론트엔드의 authorize 요청에 사용한 redirect_uri와 동일한 값", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://example.com/oauth/callback", format = "uri") @NotBlank(message = "redirect_uri는 필수입니다.") String redirectUri) {
}
