package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

@Builder
public record ModifyPersonalInfoReqDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "이름", example = "홍길동") @NotBlank String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "생년월일 (yyyy-MM-dd)", type = "string", format = "date", example = "2009-01-01") @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birth,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "성별", example = "MALE", allowableValues = {
                "MALE", "FEMALE"}) @NotNull Sex sex){
}
