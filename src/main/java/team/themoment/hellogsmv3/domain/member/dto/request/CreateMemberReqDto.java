package team.themoment.hellogsmv3.domain.member.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

public record CreateMemberReqDto(
        @NotBlank @Schema(description = "사전에 인증 완료한 가입 인증코드", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @NotBlank @Schema(description = "지원자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$") @NotBlank @Schema(description = "인증코드 전송에 사용한 하이픈 없는 전화번호. 정규식 ^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber,
        @NotNull @Schema(description = "성별", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED) Sex sex,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") @NotNull @Schema(description = "생년월일(yyyy-MM-dd)", example = "2010-01-01", requiredMode = Schema.RequiredMode.REQUIRED, format = "date") LocalDate birth) {
}
