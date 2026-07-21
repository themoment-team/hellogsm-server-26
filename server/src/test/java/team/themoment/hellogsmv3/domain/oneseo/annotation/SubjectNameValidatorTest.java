package team.themoment.hellogsmv3.domain.oneseo.annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import jakarta.validation.ConstraintValidatorContext;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;

@DisplayName("SubjectNameValidator 클래스의")
class SubjectNameValidatorTest {

    @DisplayName("isValid 메서드는")
    @Nested
    class Describe_isValid {

        SubjectNameValidator validator;
        ConstraintValidatorContext context;
        MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto;
        ValidSubjectName annotation;

        @BeforeEach
        void setUp() {
            validator = new SubjectNameValidator();
            @ValidSubjectName
            class Dummy {
            }
            annotation = Dummy.class.getAnnotation(ValidSubjectName.class);
            validator.initialize(annotation);
            context = Mockito.mock(ConstraintValidatorContext.class);
            ConstraintValidatorContext.ConstraintViolationBuilder builder = Mockito
                    .mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
            given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(builder);
            given(builder.addConstraintViolation()).willReturn(context);
        }

        @DisplayName("모든 과목이 중복되지 않으면")
        @Nested
        class Context_with_no_duplicate_subjects {

            @BeforeEach
            void setUp() {
                List<String> generalSubjects = Arrays.asList("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어");
                List<String> newSubjects = Arrays.asList("프로그래밍");
                List<String> artsPhysicalSubjects = Arrays.asList("체육", "미술", "음악");

                middleSchoolAchievementReqDto = MiddleSchoolAchievementReqDto.builder().generalSubjects(generalSubjects)
                        .newSubjects(newSubjects).artsPhysicalSubjects(artsPhysicalSubjects).build();
            }

            @DisplayName("ConstraintViolation을 발생시키지 않는다")
            @Test
            void it_doesnt_make_constraint_violation() {
                boolean result = validator.isValid(middleSchoolAchievementReqDto, context);
                assertTrue(result);
            }
        }

        @DisplayName("중복된 과목이 있으면")
        @Nested
        class Context_with_multiple_duplicates {

            @BeforeEach
            void setUp() {
                List<String> generalSubjects = Arrays.asList("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어");
                List<String> newSubjects = Arrays.asList("수학", "체육");
                List<String> artsPhysicalSubjects = Arrays.asList("체육", "미술", "음악");

                middleSchoolAchievementReqDto = MiddleSchoolAchievementReqDto.builder().generalSubjects(generalSubjects)
                        .newSubjects(newSubjects).artsPhysicalSubjects(artsPhysicalSubjects).build();
            }

            @DisplayName("ConstraintViolation을 발생시킨다")
            @Test
            void it_makes_constraint_violation() {
                boolean result = validator.isValid(middleSchoolAchievementReqDto, context);
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(annotation.message());
            }
        }

        @DisplayName("필요한 필드가 null이면")
        @Nested
        class Context_with_null_fields {

            @BeforeEach
            void setUp() {
                middleSchoolAchievementReqDto = MiddleSchoolAchievementReqDto.builder()
                        .generalSubjects(Arrays.asList("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어"))
                        .newSubjects(null).artsPhysicalSubjects(Arrays.asList("체육", "미술", "음악")).build();
            }

            @DisplayName("ConstraintViolation을 발생시키지 않는다")
            @Test
            void it_doesnt_make_constraint_violation() {
                boolean result = validator.isValid(middleSchoolAchievementReqDto, context);
                assertTrue(result);
            }
        }
    }
}
