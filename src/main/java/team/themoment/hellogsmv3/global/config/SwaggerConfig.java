package team.themoment.hellogsmv3.global.config;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.sdk.response.CommonApiResponse;

@OpenAPIDefinition(info = @Info(title = "Hello, GSM 2025", description = "광주소프트웨어마이스터고등학교 입학지원 시스템", version = "v1"))
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi api(OperationCustomizer operationCustomizer) {
        return GroupedOpenApi.builder().group("Hello, GSM 2025 API")
                .pathsToMatch("/utility/**",
                        "/member/**",
                        "/oneseo/**",
                        "/auth/**",
                        "/date",
                        "/operation/**",
                        "/test-result/**")
                .addOperationCustomizer(operationCustomizer).build();
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            removeAuthRequestParameters(operation, handlerMethod);

            Class<?> returnType = handlerMethod.getMethod().getReturnType();
            if (returnType != void.class && returnType != Void.class) {
                this.addResponseBodyWrapperSchemaExample(operation,
                        CommonApiResponse.class.isAssignableFrom(returnType));
            }

            return operation;
        };
    }

    private void removeAuthRequestParameters(Operation operation, HandlerMethod handlerMethod) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            operation.setParameters(parameters.stream().filter(param -> !isAuthRequestParameter(handlerMethod, param))
                    .collect(toList()));
        }
    }

    private boolean isAuthRequestParameter(HandlerMethod handlerMethod, Parameter parameter) {
        if ("path".equals(parameter.getIn())) {
            return false;
        }
        return Arrays.stream(handlerMethod.getMethod().getParameters())
                .anyMatch(methodParameter -> methodParameter.isAnnotationPresent(AuthRequest.class)
                        && methodParameter.getName().equals(parameter.getName()));
    }

    private void addResponseBodyWrapperSchemaExample(Operation operation, boolean hideDataField) {
        if (operation.getResponses() == null) {
            return;
        }
        operation.getResponses().forEach((code, response) -> {
            if (!code.matches("2[0-9]{2}") || response == null || response.getContent() == null) {
                return;
            }
            Set<MediaType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            response.getContent().values().forEach(mediaType -> {
                if (mediaType != null && visited.add(mediaType) && mediaType.getSchema() != null) {
                    mediaType.schema(wrapSchema(mediaType.getSchema(), hideDataField, Integer.parseInt(code)));
                }
            });
        });
    }

    @Bean
    public org.springdoc.core.customizers.GlobalOpenApiCustomizer unwrappedResponseCustomizer(
            team.themoment.sdk.config.ResponseProperties responseProperties) {
        org.springframework.util.AntPathMatcher matcher = new org.springframework.util.AntPathMatcher();
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> {
                if (responseProperties.getNotWrappingUrls().stream()
                        .noneMatch(pattern -> matcher.match(pattern, path))) {
                    return;
                }
                item.readOperations().forEach(operation -> {
                    if (operation.getResponses() == null) {
                        return;
                    }
                    operation.getResponses().forEach((statusCode, response) -> {
                        if (response == null || !statusCode.matches("2[0-9]{2}") || response.getContent() == null) {
                            return;
                        }
                        response.getContent().forEach((mediaType, media) -> {
                            if (media == null) {
                                return;
                            }
                            Schema<?> schema = media.getSchema();
                            if (schema instanceof WrappedResponseSchema wrapped) {
                                schema = wrapped.originalSchema;
                                media.setSchema(schema);
                            }
                            if (isTextMediaType(mediaType) && isEmptyObjectSchema(schema)) {
                                media.setSchema(new Schema<>().type("string").types(Set.of("string")));
                            } else if (isProtobufMediaType(mediaType)) {
                                media.setSchema(new Schema<>().type("string").types(Set.of("string")).format("binary"));
                            }
                        });
                    });
                });
            });
        };
    }

    private static boolean isTextMediaType(String mediaType) {
        String normalized = mediaType.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("text/") || normalized.startsWith("application/openmetrics-text");
    }

    private static boolean isProtobufMediaType(String mediaType) {
        return mediaType.toLowerCase(java.util.Locale.ROOT).contains("protobuf");
    }

    private static boolean isEmptyObjectSchema(Schema<?> schema) {
        return schema == null || ("object".equals(schema.getType())
                && (schema.getProperties() == null || schema.getProperties().isEmpty()));
    }

    private static final class WrappedResponseSchema extends Schema<Object> {
        private final Schema<?> originalSchema;

        private WrappedResponseSchema(Schema<?> originalSchema) {
            this.originalSchema = originalSchema;
            type("object");
            types(Set.of("object"));
        }
    }

    private Schema<?> wrapSchema(Schema<?> originalSchema, boolean hideDataField, int code) {
        final Schema<?> wrapperSchema = new WrappedResponseSchema(originalSchema);
        HttpStatus status = HttpStatus.resolve(code);
        wrapperSchema.addProperty("status",
                new Schema<>().type("string").types(Set.of("string"))
                        .example(status == null ? String.valueOf(code) : status.toString()));
        wrapperSchema.addProperty("code", new Schema<>().type("integer").types(Set.of("integer")).example(code));
        wrapperSchema.addProperty("message",
                new Schema<>().type("string").types(Set.of("string"))
                        .description("요청 처리 결과 메시지입니다. 직접 반환하는 CommonApiResponse의 메시지는 API마다 다릅니다.")
                        .example(hideDataField ? "요청을 성공적으로 처리했습니다." : "OK"));
        wrapperSchema.setRequired(new ArrayList<>(List.of("status", "code", "message")));
        if (!hideDataField) {
            wrapperSchema.addProperty("data", originalSchema);
            wrapperSchema.addRequiredItem("data");
        }
        return wrapperSchema;
    }
}
