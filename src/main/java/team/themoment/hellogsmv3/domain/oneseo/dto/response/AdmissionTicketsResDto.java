package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AdmissionTicketsResDto(@Schema(description = "지원자 이름") String memberName,
        @Schema(description = "지원자 생년월일 (yyyy-MM-dd)", type = "string", format = "date", example = "2009-01-01") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate memberBirth,
        @Schema(description = "증명사진 URL", format = "uri", example = "https://example.com/file") String profileImg,
        @Schema(description = "중학교 이름") String schoolName,
        @Schema(description = "수험번호. 발급 전에는 null", nullable = true) String examinationNumber,
        @Schema(description = "원서 접수번호") String oneseoSubmitCode) {
}
