package team.themoment.hellogsmv3.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GenerateCodeReqDto(
        @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$") @NotNull @Schema(description = "하이픈 없는 전화번호. 정규식 ^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber) {
}
