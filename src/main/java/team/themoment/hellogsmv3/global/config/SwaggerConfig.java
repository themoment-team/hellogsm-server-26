package team.themoment.hellogsmv3.global.config;

import static java.util.stream.Collectors.*;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.SneakyThrows;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;

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
            this.addResponseBodyWrapperSchemaExample(operation, CommonApiResponse.class.isAssignableFrom(returnType));

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
        Annotation[][] parameterAnnotations = handlerMethod.getMethod().getParameterAnnotations();
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (Annotation annotation : parameterAnnotation) {
                if (annotation.annotationType().equals(AuthRequest.class)) {
                    return parameter.getName() != null && parameter.getName().equals("memberId");
                }
            }
        }

        return false;
    }

    private void addResponseBodyWrapperSchemaExample(Operation operation, boolean hideDataField) {
        final Content content = operation.getResponses().get("200").getContent();
        if (content != null) {
            content.keySet().forEach(mediaTypeKey -> {
                final MediaType mediaType = content.get(mediaTypeKey);
                Schema<?> originalSchema = mediaType.getSchema();
                mediaType.schema(wrapSchema(originalSchema, hideDataField));
            });
        }
    }

    @SneakyThrows
    private Schema<?> wrapSchema(Schema<?> originalSchema, boolean hideDataField) {
        final Schema<?> wrapperSchema = new Schema<>();

        wrapperSchema.addProperty("status", new Schema<>().type("string").example("OK"));
        wrapperSchema.addProperty("code", new Schema<>().type("integer").example(200));
        wrapperSchema.addProperty("message", new Schema<>().type("string").example("OK"));
        if (!hideDataField)
            wrapperSchema.addProperty("data", originalSchema);

        return wrapperSchema;
    }
}
