package team.themoment.hellogsmv3.domain.oneseo.annotation;

import java.util.Collections;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;

public class SubjectNameValidator implements ConstraintValidator<ValidSubjectName, MiddleSchoolAchievementReqDto> {

    private ValidSubjectName annotation;

    @Override
    public void initialize(ValidSubjectName constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto,
            ConstraintValidatorContext context) {

        if (middleSchoolAchievementReqDto.generalSubjects() == null
                || middleSchoolAchievementReqDto.artsPhysicalSubjects() == null
                || middleSchoolAchievementReqDto.newSubjects() == null) {
            return true;
        }

        boolean isValid = Collections.disjoint(middleSchoolAchievementReqDto.generalSubjects(),
                middleSchoolAchievementReqDto.newSubjects())
                && Collections.disjoint(middleSchoolAchievementReqDto.artsPhysicalSubjects(),
                        middleSchoolAchievementReqDto.newSubjects())
                && Collections.disjoint(middleSchoolAchievementReqDto.artsPhysicalSubjects(),
                        middleSchoolAchievementReqDto.generalSubjects());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(annotation.message()).addConstraintViolation();
            return false;
        }
        return true;
    }
}
