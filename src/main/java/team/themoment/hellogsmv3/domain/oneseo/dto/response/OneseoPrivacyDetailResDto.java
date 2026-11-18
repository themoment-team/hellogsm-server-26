package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

@Builder
public record OneseoPrivacyDetailResDto(@Schema(description = "지원자 이름", nullable = true) String name,
        @Schema(description = "성별 (MALE, FEMALE)", nullable = true) Sex sex,
        @Schema(description = "생년월일 (yyyy-MM-dd)", nullable = true, type = "string", format = "date", example = "2009-01-01") @JsonSerialize(using = LocalDateSerializer.class) @JsonDeserialize(using = LocalDateDeserializer.class) @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birth,
        @Schema(description = "지원자 전화번호", nullable = true) String phoneNumber,
        @Schema(description = "졸업 구분 (CANDIDATE, GED, GRADUATE)", nullable = true) GraduationType graduationType,
        @Schema(description = "주소", nullable = true) String address,
        @Schema(description = "상세 주소", nullable = true) String detailAddress,
        @Schema(description = "보호자 이름", nullable = true) String guardianName,
        @Schema(description = "보호자 전화번호", nullable = true) String guardianPhoneNumber,
        @Schema(description = "보호자와의 관계", nullable = true) String relationshipWithGuardian,
        @Schema(description = "중학교 이름", nullable = true) String schoolName,
        @Schema(description = "중학교 주소", nullable = true) String schoolAddress,
        @Schema(description = "담임교사 이름", nullable = true) String schoolTeacherName,
        @Schema(description = "담임교사 전화번호", nullable = true) String schoolTeacherPhoneNumber,
        @Schema(description = "증명사진 URL", nullable = true, format = "uri", example = "https://example.com/file") String profileImg,
        @Schema(description = "중학교 졸업 연월 (yyyy-MM)", nullable = true) String graduationDate,
        @Schema(description = "학생 번호", nullable = true) String studentNumber) {
}
