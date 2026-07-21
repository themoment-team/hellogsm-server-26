package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

@Builder
public record AdmissionTicketsResDto(String memberName, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate memberBirth,
        String profileImg, String schoolName, String examinationNumber, String oneseoSubmitCode) {
}
