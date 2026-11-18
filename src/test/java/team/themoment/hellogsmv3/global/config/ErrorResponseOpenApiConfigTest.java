package team.themoment.hellogsmv3.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@DisplayName("ErrorResponseOpenApiConfig 테스트")
class ErrorResponseOpenApiConfigTest {
    @Nested
    @DisplayName("errorResponseCustomizer 메서드는")
    class Describe_errorResponseCustomizer {
        @Nested
        @DisplayName("빈 오류 응답과 명시적인 본문이 주어진 경우")
        class Context_error_responses {
            @Test
            @DisplayName("그룹에도 적용되는 인터페이스로 빈 오류 본문만 채운다")
            void it_fills_only_empty_errors() {
                Schema<?> explicit = new Schema<>().$ref("#/components/schemas/SpecificError");
                ApiResponses responses = new ApiResponses().addApiResponse("200", new ApiResponse())
                        .addApiResponse("204", new ApiResponse())
                        .addApiResponse("400", new ApiResponse().description("잘못된 입력").content(new Content()))
                        .addApiResponse("401", new ApiResponse()).addApiResponse("403", new ApiResponse())
                        .addApiResponse("500", new ApiResponse()).addApiResponse("409",
                                new ApiResponse().content(new Content().addMediaType("application/json",
                                        new MediaType().schema(explicit))));
                OpenAPI api = new OpenAPI().paths(
                        new Paths().addPathItem("/example", new PathItem().get(new Operation().responses(responses))));
                GlobalOpenApiCustomizer customizer = new ErrorResponseOpenApiConfig().errorResponseCustomizer();

                customizer.customise(api);

                assertThat(api.getComponents().getSchemas().get("ErrorResponse").getProperties())
                        .containsOnlyKeys("status", "code", "message");
                assertThat(api.getComponents().getSchemas().get("ErrorResponse").getRequired())
                        .containsExactlyInAnyOrder("status", "code", "message");
                for (String code : new String[]{"400", "401", "403", "500"}) {
                    MediaType media = responses.get(code).getContent().get("application/json");
                    assertThat(media.getSchema().get$ref()).isEqualTo("#/components/schemas/ErrorResponse");
                    assertThat(((java.util.Map<?, ?>) media.getExample()).get("code"))
                            .isEqualTo(Integer.parseInt(code));
                }
                assertThat(responses.get("400").getDescription()).isEqualTo("잘못된 입력");
                assertThat(responses.get("409").getContent().get("application/json").getSchema()).isSameAs(explicit);
                assertThat(responses.get("200").getContent()).isNull();
                assertThat(responses.get("204").getContent()).isNull();
            }
        }
    }
}
