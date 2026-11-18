package team.themoment.hellogsmv3.domain.oneseo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Sort;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.*;
import team.themoment.hellogsmv3.global.exception.GlobalExceptionHandler;

@DisplayName("원서 입력 검증 회귀 테스트")
class OneseoInputValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Nested
    @DisplayName("search 메서드는")
    class Describe_search {
        @Nested
        @DisplayName("페이지 크기가 올바르지 않은 경우")
        class Context_invalid_size {
            @ParameterizedTest
            @CsvSource({"0, 0", "0, -1", "-1, 10"})
            @DisplayName("서비스 호출 전 400으로 처리할 검증 오류를 만든다")
            void it_rejects_invalid_pagination(int page, int size) throws Exception {
                var violations = validator.forExecutables()
                        .validateParameters(controller(), search(), searchArguments(page, size));
                assertFalse(violations.isEmpty());
                var response = new GlobalExceptionHandler()
                        .validationException(new ConstraintViolationException(violations));
                assertEquals(400, response.getCode());
            }
        }

        @Nested
        @DisplayName("유효한 페이지 경계값인 경우")
        class Context_valid_boundary {
            @Test
            @DisplayName("0번째 페이지와 크기 1을 허용한다")
            void it_accepts_boundary() throws Exception {
                assertTrue(validator.forExecutables().validateParameters(controller(), search(), searchArguments(0, 1))
                        .isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("원서 등록과 수정 요청 DTO는")
    class Describe_oneseo_request {
        @Nested
        @DisplayName("성적 객체가 없는 경우")
        class Context_missing_achievement {
            @ParameterizedTest
            @EnumSource(GraduationType.class)
            @DisplayName("검정고시를 포함한 모든 졸업 구분에서 성적 객체를 필수로 검증한다")
            void it_requires_achievement(GraduationType graduationType) {
                var violations = validator.validate(request(graduationType, null));
                assertTrue(violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("middleSchoolAchievement")));
            }
        }

        @Nested
        @DisplayName("성적 객체가 주어진 경우")
        class Context_present_achievement {
            @ParameterizedTest
            @EnumSource(GraduationType.class)
            @DisplayName("객체 누락 검증을 통과한다")
            void it_accepts_present_achievement(GraduationType graduationType) {
                assertTrue(validator.validate(request(graduationType, MiddleSchoolAchievementReqDto.builder().build()))
                        .isEmpty());
            }

            @Test
            @DisplayName("부분 임시저장에는 성적 객체가 없어도 된다")
            void it_keeps_temp_storage_optional() {
                assertTrue(validator.validate(OneseoTempReqDto.builder().build()).isEmpty());
            }
        }
    }

    private static OneseoReqDto request(GraduationType type, MiddleSchoolAchievementReqDto achievement) {
        return OneseoReqDto.builder().guardianName("김보호").guardianPhoneNumber("01012345678")
                .relationshipWithGuardian("모").profileImg("https://example.com/profile.jpg").address("광주광역시")
                .detailAddress("101호").graduationType(type).graduationDate("2027-02").schoolName("광주중학교")
                .schoolAddress("광주광역시").schoolTeacherName("김교사").schoolTeacherPhoneNumber("01012345678")
                .firstDesiredMajor(Major.SW).secondDesiredMajor(Major.AI).thirdDesiredMajor(Major.IOT)
                .screening(Screening.GENERAL).middleSchoolAchievement(achievement).build();
    }

    private static OneseoController controller() throws Exception {
        var constructor = OneseoController.class.getConstructors()[0];
        return (OneseoController) constructor
                .newInstance(Arrays.stream(constructor.getParameterTypes()).map(t -> mock(t)).toArray());
    }

    private static Method search() throws Exception {
        return OneseoController.class.getMethod("search",
                Integer.class,
                Integer.class,
                TestResultTag.class,
                ScreeningCategory.class,
                YesNo.class,
                String.class,
                OneseoEditStatusTag.class,
                OneseoSortBy.class,
                Sort.Direction.class);
    }

    private static Object[] searchArguments(int page, int size) {
        return new Object[]{page, size, TestResultTag.ALL, null, null, null, null, null, Sort.Direction.ASC};
    }
}
