package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.annotation.ValidDesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;

@Builder
@ValidDesiredMajors
public record OneseoReqDto(@Schema(description = "보호자 이름", defaultValue = "김보호") @NotBlank String guardianName,
        @Schema(description = "보호자 전화번호", defaultValue = "01000000000") @NotBlank @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", message = "유효한 전화번호가 아닙니다.") String guardianPhoneNumber,
        @Schema(description = "보호자와 관계", defaultValue = "모") @NotBlank String relationshipWithGuardian,
        @Schema(description = "증명사진 URL", defaultValue = "https://abc.com") @NotBlank @Pattern(regexp = "^https://[^\\s/$.?#].[^\\s]*$", message = "유효한 이미지 URL이 아닙니다.") String profileImg,
        @Schema(description = "주소", defaultValue = "광주광역시 광산구 송정동 상무대로 312") @NotBlank String address,
        @Schema(description = "상세주소", defaultValue = "101동 1001호") @NotBlank String detailAddress,
        @Schema(description = "지원자 졸업상태", defaultValue = "CANDIDATE", allowableValues = {
                "CANDIDATE", "GED", "GRADUATE"}) @NotNull GraduationType graduationType,
        @Schema(description = "담임선생님 이름", defaultValue = "김선생") String schoolTeacherName,
        @Schema(description = "담임선생님 전화번호", nullable = true, defaultValue = "01000000000") @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", message = "유효한 전화번호가 아닙니다.") String schoolTeacherPhoneNumber,
        @Schema(description = "1지망 학과", defaultValue = "SW", allowableValues = {"SW", "AI",
                "IOT"}) @NotNull Major firstDesiredMajor,
        @Schema(description = "2지망 학과", defaultValue = "AI", allowableValues = {"SW", "AI",
                "IOT"}) @NotNull Major secondDesiredMajor,
        @Schema(description = "3지망 학과", defaultValue = "IOT", allowableValues = {"SW", "AI",
                "IOT"}) @NotNull Major thirdDesiredMajor,
        @Valid MiddleSchoolAchievementReqDto middleSchoolAchievement,
        @Schema(description = "중학교 이름", nullable = true, defaultValue = "금호중앙중학교") String schoolName,
        @Schema(description = "중학교 주소", nullable = true, defaultValue = "광주광역시 북구 운암2동 금호로 100") String schoolAddress,
        @Schema(description = "자원자 전형", defaultValue = "GENERAL", allowableValues = {"GENERAL", "SPECIAL",
                "EXTRA_VETERANS", "EXTRA_ADMISSION"}) @NotNull Screening screening,
        @Schema(description = "중학교 졸업년월", defaultValue = "2006-03") @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$") @NotNull String graduationDate,
        @Schema(description = "학생 번호", defaultValue = "30508") @Pattern(regexp = "^\\d{5}$") String studentNumber){
}
