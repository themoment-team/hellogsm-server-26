package team.themoment.hellogsmv3.domain.oneseo.annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Major.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.validation.ConstraintValidatorContext;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;

@DisplayName("DesiredMajorsValidator 클래스의")
class DesiredMajorsValidatorTest {

    private DesiredMajorsValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new DesiredMajorsValidator();

        @ValidDesiredMajors
        class Dummy {
        }
        ValidDesiredMajors annotation = Dummy.class.getAnnotation(ValidDesiredMajors.class);
        validator.initialize(annotation);

        context = Mockito.mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = Mockito
                .mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        given(context.buildConstraintViolationWithTemplate(anyString())).willReturn(builder);
        given(builder.addConstraintViolation()).willReturn(context);
    }

    @Nested
    @DisplayName("isValid 메서드는")
    class Describe_isValid {

        private OneseoReqDto createValidOneseoReqDto(Major first, Major second, Major third) {
            return OneseoReqDto.builder().guardianName("김보호").guardianPhoneNumber("01000000000")
                    .relationshipWithGuardian("모").profileImg("https://example.com/image.jpg")
                    .address("광주광역시 광산구 송정동 상무대로 312").detailAddress("101동 1001호").graduationType(CANDIDATE)
                    .schoolTeacherName("김선생").schoolTeacherPhoneNumber("01000000000").firstDesiredMajor(first)
                    .secondDesiredMajor(second).thirdDesiredMajor(third)
                    .middleSchoolAchievement(mock(MiddleSchoolAchievementReqDto.class)).schoolName("금호중앙중학교")
                    .schoolAddress("광주광역시 북구 운암2동 금호로 100").screening(GENERAL).graduationDate("2006-03").build();
        }

        @Nested
        @DisplayName("모든 전공이 서로 다르게 주어지면")
        class Context_with_all_different_majors {

            private OneseoReqDto validDto;

            @BeforeEach
            void setUp() {
                validDto = createValidOneseoReqDto(SW, AI, IOT);
            }

            @Test
            @DisplayName("true를 반환한다")
            void it_returns_true() {
                boolean result = validator.isValid(validDto, context);

                assertTrue(result);
                verify(context, never()).disableDefaultConstraintViolation();
                verify(context, never()).buildConstraintViolationWithTemplate(anyString());
            }
        }

        @Nested
        @DisplayName("첫 번째 전공이 null로 주어지면")
        class Context_with_first_major_null {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(null, AI, IOT);
            }

            @Test
            @DisplayName("false를 반환하고 NULL 에러 메시지를 설정한다")
            void it_returns_false_and_sets_null_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("모든 필드는 NULL일 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("두 번째 전공이 null로 주어지면")
        class Context_with_second_major_null {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(SW, null, IOT);
            }

            @Test
            @DisplayName("false를 반환하고 NULL 에러 메시지를 설정한다")
            void it_returns_false_and_sets_null_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("모든 필드는 NULL일 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("세 번째 전공이 null로 주어지면")
        class Context_with_third_major_null {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(SW, AI, null);
            }

            @Test
            @DisplayName("false를 반환하고 NULL 에러 메시지를 설정한다")
            void it_returns_false_and_sets_null_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("모든 필드는 NULL일 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("모든 전공이 null로 주어지면")
        class Context_with_all_majors_null {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(null, null, null);
            }

            @Test
            @DisplayName("false를 반환하고 NULL 에러 메시지를 설정한다")
            void it_returns_false_and_sets_null_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("모든 필드는 NULL일 수 없습니다.");
            }
        }

        @Nested
        @DisplayName("첫 번째와 두 번째 전공이 중복되면")
        class Context_with_first_and_second_major_duplicated {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(SW, SW, IOT);
            }

            @Test
            @DisplayName("false를 반환하고 중복 에러 메시지를 설정한다")
            void it_returns_false_and_sets_duplicate_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("중복된 전공이 입력되었습니다.");
            }
        }

        @Nested
        @DisplayName("첫 번째와 세 번째 전공이 중복되면")
        class Context_with_first_and_third_major_duplicated {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(SW, AI, SW);
            }

            @Test
            @DisplayName("false를 반환하고 중복 에러 메시지를 설정한다")
            void it_returns_false_and_sets_duplicate_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("중복된 전공이 입력되었습니다.");
            }
        }

        @Nested
        @DisplayName("두 번째와 세 번째 전공이 중복되면")
        class Context_with_second_and_third_major_duplicated {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(SW, AI, AI);
            }

            @Test
            @DisplayName("false를 반환하고 중복 에러 메시지를 설정한다")
            void it_returns_false_and_sets_duplicate_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("중복된 전공이 입력되었습니다.");
            }
        }

        @Nested
        @DisplayName("모든 전공이 동일하게 주어지면")
        class Context_with_all_majors_same {

            private OneseoReqDto invalidDto;

            @BeforeEach
            void setUp() {
                invalidDto = createValidOneseoReqDto(SW, SW, SW);
            }

            @Test
            @DisplayName("false를 반환하고 중복 에러 메시지를 설정한다")
            void it_returns_false_and_sets_duplicate_error_message() {
                boolean result = validator.isValid(invalidDto, context);

                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate("중복된 전공이 입력되었습니다.");
            }
        }
    }
}
