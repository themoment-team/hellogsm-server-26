package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import lombok.Builder;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

@Builder
public record OneseoPrivacyDetailResDto(String name, Sex sex,
        @JsonSerialize(using = LocalDateSerializer.class) @JsonDeserialize(using = LocalDateDeserializer.class) @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birth,
        String phoneNumber, GraduationType graduationType, String address, String detailAddress, String guardianName,
        String guardianPhoneNumber, String relationshipWithGuardian, String schoolName, String schoolAddress,
        String schoolTeacherName, String schoolTeacherPhoneNumber, String profileImg, String graduationDate,
        String studentNumber) {
}
