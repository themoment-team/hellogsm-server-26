package team.themoment.hellogsmv3.domain.oneseo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = SubjectNameValidator.class)
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSubjectName {
    String message() default "중복된 이름의 과목이 입력되었습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
