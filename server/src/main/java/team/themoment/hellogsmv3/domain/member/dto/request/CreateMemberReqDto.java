package team.themoment.hellogsmv3.domain.member.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

public record CreateMemberReqDto(@NotBlank String code, @NotBlank String name,
        @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$") @NotBlank String phoneNumber, @NotNull Sex sex,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") @NotNull LocalDate birth) {
}
