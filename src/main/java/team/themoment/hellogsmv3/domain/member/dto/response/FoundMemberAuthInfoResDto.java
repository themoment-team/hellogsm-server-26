package team.themoment.hellogsmv3.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;

@Builder
public record FoundMemberAuthInfoResDto(
        @Schema(description = "회원 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long memberId,
        @Schema(description = "OAuth 회원 식별 정보. Kakao 이메일 미제공 시 제공자의 사용자 ID 문자열", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(description = "OAuth 가입 제공자", example = "GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED) AuthReferrerType authReferrerType,
        @Schema(description = "회원 역할", example = "UNAUTHENTICATED", requiredMode = Schema.RequiredMode.REQUIRED) Role role) {
}
