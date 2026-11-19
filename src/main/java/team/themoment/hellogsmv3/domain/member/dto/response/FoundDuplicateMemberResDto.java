package team.themoment.hellogsmv3.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public record FoundDuplicateMemberResDto(
        @Schema(description = "동일 전화번호의 가입 회원 존재 여부", example = "YES", requiredMode = Schema.RequiredMode.REQUIRED) YesNo duplicateMemberYn) {
}
