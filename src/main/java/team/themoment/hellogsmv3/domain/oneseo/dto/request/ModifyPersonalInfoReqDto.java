package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

@Builder
public record ModifyPersonalInfoReqDto(@Schema(description = "이름", defaultValue = "홍길동") @NotBlank String name,
        @Schema(description = "생년월일", defaultValue = "2009-01-01") @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birth,
        @Schema(description = "성별", defaultValue = "MALE", allowableValues = {
                "MALE", "FEMALE"}) @NotNull Sex sex){
}
