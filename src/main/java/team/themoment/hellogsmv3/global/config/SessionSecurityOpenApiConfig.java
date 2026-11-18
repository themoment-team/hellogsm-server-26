package team.themoment.hellogsmv3.global.config;

import java.util.List;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import team.themoment.hellogsmv3.global.security.data.SecurityRoutes;

@Configuration
public class SessionSecurityOpenApiConfig {

    @Bean
    public GlobalOpenApiCustomizer sessionSecurityCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes("sessionCookie",
                    new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("SESSION")
                            .description("로그인 성공 후 발급되는 SESSION 쿠키입니다. 같은 출처의 요청에는 브라우저가 쿠키를 전송합니다."));
            openApi.setSecurity(List.of(new SecurityRequirement().addList("sessionCookie")));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (SecurityRoutes.isPublicRequest(HttpMethod.valueOf(method.name()), path)) {
                    operation.setSecurity(List.of());
                }
            }));
        };
    }
}
