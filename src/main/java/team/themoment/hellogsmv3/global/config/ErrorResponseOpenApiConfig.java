package team.themoment.hellogsmv3.global.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

@Configuration
public class ErrorResponseOpenApiConfig {

    @Bean
    public GlobalOpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Schema<?> error = new Schema<>().type("object").types(Set.of("object"))
                    .description("오류 응답입니다. 검증 오류의 필드별 상세 내용도 message 문자열에 포함됩니다.");
            error.addProperty("status",
                    new Schema<>().type("string").types(Set.of("string")).description("HTTP 상태 코드와 상태 이름입니다.")
                            .example("400 BAD_REQUEST"));
            error.addProperty("code", new Schema<>().type("integer").types(Set.of("integer")).example(400));
            error.addProperty("message",
                    new Schema<>().type("string").types(Set.of("string")).description("오류 원인입니다.")
                            .example("잘못된 요청입니다."));
            error.setRequired(List.of("status", "code", "message"));
            openApi.getComponents().addSchemas("ErrorResponse", error);
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getResponses() == null) {
                    return;
                }
                operation.getResponses().forEach((code, response) -> {
                    if (!code.matches("[45][0-9]{2}") || response == null
                            || (response.getContent() != null && !response.getContent().isEmpty())) {
                        return;
                    }
                    int statusCode = Integer.parseInt(code);
                    HttpStatus status = HttpStatus.resolve(statusCode);
                    response.setContent(new Content().addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                    .example(Map.of("status",
                                            status == null ? code : status.toString(),
                                            "code",
                                            statusCode,
                                            "message",
                                            response.getDescription() == null
                                                    ? "요청을 처리할 수 없습니다."
                                                    : response.getDescription()))));
                });
            }));
        };
    }
}
