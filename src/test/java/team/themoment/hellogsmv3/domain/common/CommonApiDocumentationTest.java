package team.themoment.hellogsmv3.domain.common;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import team.themoment.hellogsmv3.domain.common.date.controller.DateController;
import team.themoment.hellogsmv3.domain.common.date.dto.DateResDto;
import team.themoment.hellogsmv3.domain.common.operation.controller.OperationTestResultController;
import team.themoment.hellogsmv3.domain.common.utility.controller.UtilityController;

@DisplayName("공통 API 문서 테스트")
class CommonApiDocumentationTest {
    @Nested
    @DisplayName("공통 엔드포인트 문서는")
    class Describe_documentation {
        @Nested
        @DisplayName("실제 응답과 접근 조건을 설명할 때")
        class Context_actual_contract {
            @Test
            @DisplayName("일정의 KST 리터럴 Z 형식을 명시한다")
            void it_documents_kst_literal_z() throws Exception {
                assertNotNull(DateController.class.getDeclaredMethod("getDate").getAnnotation(ApiResponse.class));
                for (var field : DateResDto.class.getDeclaredFields()) {
                    Schema schema = field.getAnnotation(Schema.class);
                    assertNotNull(schema);
                    assertTrue(schema.description().contains("UTC가 아닙니다"));
                    assertEquals("", schema.format());
                    assertTrue(schema.example().matches(schema.pattern()));
                }
            }

            @Test
            @DisplayName("결과 운영 API의 권한과 오류 코드를 명시한다")
            void it_documents_operation_errors() {
                for (Method method : OperationTestResultController.class.getDeclaredMethods()) {
                    assertTrue(method.getAnnotation(Operation.class).description().contains("권한: ADMIN, ROOT"));
                    Set<String> codes = codes(method);
                    assertTrue(codes.containsAll(Set.of("200", "401", "403", "404")));
                    if (!method.getName().equals("queryTestResult")) {
                        assertTrue(codes.contains("400"));
                    }
                }
            }

            @Test
            @DisplayName("유틸리티의 비운영 공개 접근과 입력 오류를 명시한다")
            void it_documents_public_utility() {
                for (Method method : UtilityController.class.getDeclaredMethods()) {
                    String description = method.getAnnotation(Operation.class).description();
                    assertTrue(description.contains("prod 프로파일에서는 컨트롤러가 등록되지 않습니다"));
                    assertTrue(description.contains("인증 없이"));
                    assertEquals(Set.of("200", "400", "404"), codes(method));
                }
            }
        }
    }

    private static Set<String> codes(Method method) {
        return Arrays.stream(method.getAnnotation(ApiResponses.class).value()).map(ApiResponse::responseCode)
                .collect(Collectors.toSet());
    }
}
