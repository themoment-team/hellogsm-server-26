package team.themoment.hellogsmv3.global.config;

import java.util.Arrays;
import java.util.List;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.hellogsmv3.global.security.data.SecurityRoutes;

@Configuration
public class SessionSecurityOpenApiConfig {
    public static final String SESSION_COOKIE_SCHEME = "sessionCookie";

    /** 보안 매처가 없어도 @AuthRequest 리졸버가 익명 요청에 401을 반환하므로 인증이 필요한 API로 표시합니다. */
    @Bean
    public GlobalOperationCustomizer authRequestSecurityCustomizer() {
        return (operation, handlerMethod) -> {
            boolean requiresAuthentication = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(AuthRequest.class));
            if (requiresAuthentication && operation.getSecurity() == null) {
                operation.setSecurity(List.of(new SecurityRequirement().addList(SESSION_COOKIE_SCHEME)));
            }
            return operation;
        };
    }

    @Bean
    public GlobalOpenApiCustomizer sessionSecurityCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(SESSION_COOKIE_SCHEME,
                    new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("SESSION")
                            .description("로그인 성공 후 발급되는 SESSION 쿠키입니다. 같은 출처의 요청에는 브라우저가 쿠키를 전송합니다."));
            openApi.setSecurity(List.of(new SecurityRequirement().addList(SESSION_COOKIE_SCHEME)));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (operation.getSecurity() == null
                        && SecurityRoutes.isPublicRequest(HttpMethod.valueOf(method.name()), path)) {
                    operation.setSecurity(List.of());
                }
            }));
        };
    }
}
