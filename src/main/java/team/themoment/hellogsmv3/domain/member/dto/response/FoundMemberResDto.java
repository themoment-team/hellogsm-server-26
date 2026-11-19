package team.themoment.hellogsmv3.domain.member.dto.response;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

@Builder
public record FoundMemberResDto(
        @Schema(description = "회원 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long memberId,
        @Schema(description = "이름. 가입 전에는 null", example = "홍길동", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) String name,
        @Schema(description = "전화번호. 가입 전에는 null", example = "01012345678", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) String phoneNumber,
        @Schema(description = "생년월일. 가입 전에는 null", example = "2010-01-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, format = "date") LocalDate birth,
        @Schema(description = "성별. 가입 전에는 null", example = "MALE", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) Sex sex) {
}
