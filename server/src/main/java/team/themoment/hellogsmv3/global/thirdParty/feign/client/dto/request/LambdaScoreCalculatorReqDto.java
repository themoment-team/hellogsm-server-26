package team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request;

import java.math.BigDecimal;
import java.util.List;

import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

public record LambdaScoreCalculatorReqDto(List<Integer> achievement1_1, List<Integer> achievement1_2,
        List<Integer> achievement2_1, List<Integer> achievement2_2, List<Integer> achievement3_1,
        List<Integer> achievement3_2, List<String> generalSubjects, List<String> newSubjects,
        List<Integer> artsPhysicalAchievement, List<String> artsPhysicalSubjects, List<Integer> absentDays,
        List<Integer> attendanceDays, List<Integer> volunteerTime, String liberalSystem, String freeSemester,
        BigDecimal gedAvgScore, GraduationType graduationType) {
    public static LambdaScoreCalculatorReqDto from(MiddleSchoolAchievementReqDto dto, GraduationType graduationType) {
        return new LambdaScoreCalculatorReqDto(dto.achievement1_1(),
                dto.achievement1_2(),
                dto.achievement2_1(),
                dto.achievement2_2(),
                dto.achievement3_1(),
                dto.achievement3_2(),
                dto.generalSubjects(),
                dto.newSubjects(),
                dto.artsPhysicalAchievement(),
                dto.artsPhysicalSubjects(),
                dto.absentDays(),
                dto.attendanceDays(),
                dto.volunteerTime(),
                dto.liberalSystem(),
                dto.freeSemester(),
                dto.gedAvgScore(),
                graduationType);
    }
}
