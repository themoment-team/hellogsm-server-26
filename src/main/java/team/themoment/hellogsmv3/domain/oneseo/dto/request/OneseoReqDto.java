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
public record OneseoReqDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "보호자 이름", example = "김보호") @NotBlank String guardianName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "보호자 전화번호", example = "01000000000") @NotBlank @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", message = "유효한 전화번호가 아닙니다.") String guardianPhoneNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "보호자와 관계", example = "모") @NotBlank String relationshipWithGuardian,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "증명사진 URL", example = "https://abc.com") @NotBlank @Pattern(regexp = "^https://[^\\s/$.?#].[^\\s]*$", message = "유효한 이미지 URL이 아닙니다.") String profileImg,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "주소", example = "광주광역시 광산구 송정동 상무대로 312") @NotBlank String address,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "상세주소", example = "101동 1001호") @NotBlank String detailAddress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "지원자 졸업상태", example = "CANDIDATE", allowableValues = {
                "CANDIDATE", "GED", "GRADUATE"}) @NotNull GraduationType graduationType,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "졸업예정자(CANDIDATE)는 필수. 담임선생님 이름", example = "김선생") String schoolTeacherName,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "졸업예정자(CANDIDATE)는 필수. 담임선생님 전화번호", nullable = true, example = "01000000000") @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", message = "유효한 전화번호가 아닙니다.") String schoolTeacherPhoneNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "세 지망 학과는 서로 달라야 합니다. 1지망 학과", example = "SW", allowableValues = {
                "SW", "AI", "IOT"}) @NotNull Major firstDesiredMajor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "세 지망 학과는 서로 달라야 합니다. 2지망 학과", example = "AI", allowableValues = {
                "SW", "AI", "IOT"}) @NotNull Major secondDesiredMajor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "세 지망 학과는 서로 달라야 합니다. 3지망 학과", example = "IOT", allowableValues = {
                "SW", "AI", "IOT"}) @NotNull Major thirdDesiredMajor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "중학교 성적 객체. GED도 객체는 필수이며 gedAvgScore를 입력합니다.") @NotNull(message = "중학교 성적 정보는 필수입니다.") @Valid MiddleSchoolAchievementReqDto middleSchoolAchievement,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "졸업예정자(CANDIDATE)는 필수. 중학교 이름", nullable = true, example = "금호중앙중학교") String schoolName,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "졸업예정자(CANDIDATE)는 필수. 중학교 주소", nullable = true, example = "광주광역시 북구 운암2동 금호로 100") String schoolAddress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "지원자 전형", example = "GENERAL", allowableValues = {
                "GENERAL", "SPECIAL", "EXTRA_VETERANS", "EXTRA_ADMISSION"}) @NotNull Screening screening,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "중학교 졸업년월", example = "2006-03") @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$") @NotNull String graduationDate,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "학생 번호", example = "30508") @Pattern(regexp = "^\\d{5}$") String studentNumber){
}
