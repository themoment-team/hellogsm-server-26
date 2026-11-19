package team.themoment.hellogsmv3.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import team.themoment.sdk.response.CommonApiResponse;

@DisplayName("SwaggerConfig 응답 래퍼 테스트")
class SwaggerConfigTest {
    private final SwaggerConfig config = new SwaggerConfig();

    @Nested
    @DisplayName("operationCustomizer와 responseWrapperCustomizer는")
    class Describe_responseWrapper {
        @Nested
        @DisplayName("201 응답에 여러 미디어 타입이 같은 객체를 공유하는 경우")
        class Context_created {
            @Test
            @DisplayName("상태별 예시로 정확히 한 번 감싼다")
            void it_wraps_success_once() throws Exception {
                Schema<?> dto = new Schema<>().$ref("#/components/schemas/FoundDto");
                MediaType shared = new MediaType().schema(dto);
                Operation operation = new Operation().responses(new ApiResponses().addApiResponse("201",
                        new ApiResponse().content(
                                new Content().addMediaType("application/json", shared).addMediaType("*/*", shared))));
                customize(operation, handler("read"));
                Schema<?> wrapper = shared.getSchema();
                assertThat(wrapper.getProperties().get("data")).isSameAs(dto);
                assertThat(wrapper.getProperties().get("status").getExample()).isEqualTo("201 CREATED");
                assertThat(wrapper.getProperties().get("code").getExample()).isEqualTo(201);
                assertThat(wrapper.getTypes()).contains("object");
                assertThat(wrapper.getRequired()).contains("status", "code", "message", "data");
                assertThat(operation.getExtensions()).isNull();
            }
        }
        @Nested
        @DisplayName("빈 본문이나 오류 응답 또는 void 핸들러인 경우")
        class Context_not_wrappable {
            @Test
            @DisplayName("없는 200에 접근하지 않고 본문을 유지한다")
            void it_preserves_empty_error_and_void() throws Exception {
                Operation operation = new Operation()
                        .responses(new ApiResponses().addApiResponse("204", new ApiResponse()).addApiResponse("400",
                                new ApiResponse().content(new Content())));
                assertThatCode(() -> customize(operation, handler("read"))).doesNotThrowAnyException();
                assertThat(operation.getResponses().get("204").getContent()).isNull();
                assertThat(operation.getResponses().get("400").getContent()).isEmpty();
                Schema<?> binary = new Schema<>().type("string").format("binary");
                Operation download = operation("200", binary);
                customize(download, handler("download"));
                assertThat(download.getResponses().get("200").getContent().get("application/json").getSchema())
                        .isSameAs(binary);
            }
        }
        @Nested
        @DisplayName("CommonApiResponse를 직접 반환하는 경우")
        class Context_common_response {
            @Test
            @DisplayName("data 필드를 만들지 않는다")
            void it_hides_data() throws Exception {
                Operation operation = operation("200", new Schema<>().$ref("#/components/schemas/CommonApiResponse"));
                customize(operation, handler("write"));
                assertThat(operation.getResponses().get("200").getContent().get("application/json").getSchema()
                        .getProperties()).containsOnlyKeys("status", "code", "message");
            }
        }
    }

    @Nested
    @DisplayName("notWrappingUrls 패턴에 해당하는 경우")
    class Context_excluded_paths {
        @Test
        @DisplayName("2xx의 텍스트 미디어만 빈 스키마를 문자열로 보정하고 protobuf는 binary로 문서화한다")
        void it_normalizes_text_and_protobuf_schemas_only_for_excluded_success_responses() throws Exception {
            var paths = new io.swagger.v3.oas.models.Paths();
            var originalJson = new Schema<>().type("object");
            var jsonOperation = new Operation().responses(new ApiResponses().addApiResponse("200",
                    new ApiResponse().content(
                            new Content().addMediaType("application/json", new MediaType().schema(originalJson)))));
            var textOperation = new Operation().responses(new ApiResponses()
                    .addApiResponse("200",
                            new ApiResponse().content(new Content()
                                    .addMediaType("text/plain;version=0.0.4;charset=utf-8",
                                            new MediaType().schema(new Schema<>().type("object")))
                                    .addMediaType("application/openmetrics-text;version=1.0.0;charset=utf-8",
                                            new MediaType())
                                    .addMediaType("application/x-protobuf",
                                            new MediaType().schema(new Schema<>().type("object")))))
                    .addApiResponse("400",
                            new ApiResponse().content(new Content().addMediaType("text/plain",
                                    new MediaType().schema(new Schema<>().type("object"))))));
            paths.addPathItem("/excluded", new io.swagger.v3.oas.models.PathItem().get(textOperation));
            paths.addPathItem("/ordinary", new io.swagger.v3.oas.models.PathItem().get(jsonOperation));
            config.responseWrapperCustomizer(
                    new team.themoment.sdk.config.ResponseProperties(true, java.util.List.of("/excluded")))
                    .customise(new io.swagger.v3.oas.models.OpenAPI().paths(paths));

            var content = textOperation.getResponses().get("200").getContent();
            assertThat(content.get("text/plain;version=0.0.4;charset=utf-8").getSchema().getType()).isEqualTo("string");
            assertThat(content.get("application/openmetrics-text;version=1.0.0;charset=utf-8").getSchema().getType())
                    .isEqualTo("string");
            assertThat(content.get("application/x-protobuf").getSchema().getFormat()).isEqualTo("binary");
            assertThat(textOperation.getResponses().get("400").getContent().get("text/plain").getSchema().getType())
                    .isEqualTo("object");
            assertThat(textOperation.getResponses().get("400").getContent().get("text/plain").getSchema())
                    .isNotSameAs(null);
            assertThat(jsonOperation.getResponses().get("200").getContent().get("application/json").getSchema())
                    .isSameAs(originalJson);
        }

        @Test
        @DisplayName("제외 경로는 감싸지 않고 일반 응답은 여러 번 적용해도 한 번만 감싼다")
        void it_preserves_original_schemas() throws Exception {
            var paths = new io.swagger.v3.oas.models.Paths();
            var originals = new java.util.LinkedHashMap<String, Schema<?>>();

            for (String path : java.util.List
                    .of("/hello-management/prometheus", "/hello-management/prometheus/nested", "/exact", "/ordinary")) {
                Schema<?> original = new Schema<>().type("string").format("text");
                originals.put(path, original);
                Operation operation = operation("200", original);
                config.operationCustomizer().customize(operation, handler("read"));
                paths.addPathItem(path, new io.swagger.v3.oas.models.PathItem().get(operation));
            }
            var document = new io.swagger.v3.oas.models.OpenAPI().paths(paths);
            var customizer = config.responseWrapperCustomizer(new team.themoment.sdk.config.ResponseProperties(true,
                    java.util.List.of("/hello-management/prometheus/**", "/exact")));
            customizer.customise(document);
            customizer.customise(document);
            originals.forEach((path, original) -> {
                assertThat(paths.get(path).getGet().getExtensions()).isNull();
                Schema<?> actual = paths.get(path).getGet().getResponses().get("200").getContent()
                        .get("application/json").getSchema();
                if (path.equals("/ordinary")) {
                    assertThat(actual.getProperties().get("data")).isSameAs(original);
                } else {
                    assertThat(actual).isSameAs(original);
                }
            });
        }
    }

    @Test
    @DisplayName("AuthRequest 파라미터만 숨기고 같은 이름의 경로 파라미터는 보존한다")
    void it_preserves_path_parameter() throws Exception {
        Operation operation = new Operation().parameters(
                java.util.List.of(new io.swagger.v3.oas.models.parameters.Parameter().name("actorId").in("query"),
                        new io.swagger.v3.oas.models.parameters.Parameter().name("memberId").in("path")));
        config.operationCustomizer().customize(operation,
                new HandlerMethod(new Handlers(),
                        Handlers.class.getDeclaredMethod("authorized", Long.class, Long.class)));
        assertThat(operation.getParameters()).extracting(io.swagger.v3.oas.models.parameters.Parameter::getName)
                .containsExactly("memberId");
    }

    private void customize(Operation operation, HandlerMethod handler) {
        config.operationCustomizer().customize(operation, handler);
        var paths = new io.swagger.v3.oas.models.Paths();
        paths.addPathItem("/ordinary", new io.swagger.v3.oas.models.PathItem().get(operation));
        config.responseWrapperCustomizer(new team.themoment.sdk.config.ResponseProperties(true, java.util.List.of()))
                .customise(new io.swagger.v3.oas.models.OpenAPI().paths(paths));
    }

    private static Operation operation(String status, Schema<?> schema) {
        return new Operation().responses(new ApiResponses().addApiResponse(status,
                new ApiResponse()
                        .content(new Content().addMediaType("application/json", new MediaType().schema(schema)))));
    }

    private static HandlerMethod handler(String name) throws Exception {
        return new HandlerMethod(new Handlers(), Handlers.class.getDeclaredMethod(name));
    }

    static class Handlers {
        public String authorized(@team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest Long actorId,
                @org.springframework.web.bind.annotation.PathVariable Long memberId) {
            return "";
        }
        public String read() {
            return "";
        }
        public CommonApiResponse write() {
            return null;
        }
        public void download() {
        }
    }
}
