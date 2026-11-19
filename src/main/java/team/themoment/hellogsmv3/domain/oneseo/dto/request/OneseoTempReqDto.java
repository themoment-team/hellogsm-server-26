package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;

@Builder
public record OneseoTempReqDto(
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "보호자 이름", example = "김보호") String guardianName,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "보호자 전화번호", example = "01000000000") @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", message = "유효한 전화번호가 아닙니다.") String guardianPhoneNumber,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "보호자와 관계", example = "모") String relationshipWithGuardian,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "증명사진 URL", example = "https://abc.com") @Pattern(regexp = "^https:\\/\\/[^\\s/$.?#].[^\\s]*$", message = "유효한 이미지 URL이 아닙니다.") String profileImg,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "주소", example = "광주광역시 광산구 송정동 상무대로 312") String address,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "상세주소", example = "101동 1001호") String detailAddress,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "지원자 졸업상태", example = "CANDIDATE", allowableValues = {
                "CANDIDATE", "GED", "GRADUATE"}) GraduationType graduationType,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "담임선생님 이름", example = "김선생") String schoolTeacherName,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "담임선생님 전화번호", nullable = true, example = "01000000000") @Pattern(regexp = "^0(?:\\d|\\d{2})(?:\\d{3}|\\d{4})\\d{4}$", message = "유효한 전화번호가 아닙니다.") String schoolTeacherPhoneNumber,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "1지망 학과", example = "SW", allowableValues = {
                "SW", "AI", "IOT"}) Major firstDesiredMajor,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "2지망 학과", example = "AI", allowableValues = {
                "SW", "AI", "IOT"}) Major secondDesiredMajor,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "3지망 학과", example = "IOT", allowableValues = {
                "SW", "AI", "IOT"}) Major thirdDesiredMajor,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "부분 임시저장 성적. 생략할 수 있으며 입력한 항목만 저장합니다.") @Valid MiddleSchoolAchievementReqDto middleSchoolAchievement,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "중학교 이름", nullable = true, example = "금호중앙중학교") String schoolName,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "중학교 주소", nullable = true, example = "광주광역시 북구 운암2동 금호로 100") String schoolAddress,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "지원자 전형", example = "GENERAL", allowableValues = {
                "GENERAL", "SPECIAL", "EXTRA_VETERANS", "EXTRA_ADMISSION"}) Screening screening,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "중학교 졸업년월", example = "2006-03") @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$") String graduationDate,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "학생 번호", example = "30508") String studentNumber){
}
