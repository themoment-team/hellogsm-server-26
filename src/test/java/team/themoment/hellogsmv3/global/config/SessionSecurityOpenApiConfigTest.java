package team.themoment.hellogsmv3.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;

@DisplayName("SessionSecurityOpenApiConfig 테스트")
class SessionSecurityOpenApiConfigTest {
    @Nested
    @DisplayName("sessionSecurityCustomizer 메서드는")
    class Describe_sessionSecurityCustomizer {
        @Nested
        @DisplayName("공개 경로와 권한이 필요한 경로가 함께 주어진 경우")
        class Context_mixed_routes {
            @Test
            @DisplayName("SESSION 쿠키를 선언하고 공개 operation만 security를 빈 배열로 만든다")
            void it_matches_security_config() {
                Map<String, PathItem> routes = Map.ofEntries(Map.entry("/date", new PathItem().get(new Operation())),
                        Map.entry("/auth/v3/auth/{provider}", new PathItem().post(new Operation())),
                        Map.entry("/auth/v3/logout", new PathItem().get(new Operation())),
                        Map.entry("/utility/v3/member", new PathItem().delete(new Operation())),
                        Map.entry("/oneseo/v3/temp-storage", new PathItem().post(new Operation())),
                        Map.entry("/member/v3/member/me", new PathItem().get(new Operation()).post(new Operation())),
                        Map.entry("/oneseo/v3/entrance-intention/{memberId}", new PathItem().patch(new Operation())),
                        Map.entry("/oneseo/v3/extraction/middle-school", new PathItem().post(new Operation())),
                        Map.entry("/operation/v3/operation/status", new PathItem().get(new Operation())),
                        Map.entry("/unmatched", new PathItem().get(new Operation())));
                Paths paths = new Paths();
                routes.forEach(paths::addPathItem);
                OpenAPI api = new OpenAPI().paths(paths);
                GlobalOpenApiCustomizer customizer = new SessionSecurityOpenApiConfig().sessionSecurityCustomizer();

                customizer.customise(api);

                SecurityScheme cookie = api.getComponents().getSecuritySchemes().get("sessionCookie");
                assertThat(cookie.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
                assertThat(cookie.getIn()).isEqualTo(SecurityScheme.In.COOKIE);
                assertThat(cookie.getName()).isEqualTo("SESSION");
                assertThat(api.getSecurity()).hasSize(1);
                assertThat(api.getSecurity().getFirst()).containsKey("sessionCookie");
                for (String path : new String[]{"/date", "/auth/v3/auth/{provider}", "/auth/v3/logout",
                        "/utility/v3/member", "/oneseo/v3/temp-storage", "/unmatched"}) {
                    paths.get(path).readOperations()
                            .forEach(operation -> assertThat(operation.getSecurity()).isEmpty());
                }
                for (String path : new String[]{"/member/v3/member/me", "/oneseo/v3/entrance-intention/{memberId}",
                        "/oneseo/v3/extraction/middle-school", "/operation/v3/operation/status"}) {
                    paths.get(path).readOperations().forEach(operation -> assertThat(operation.getSecurity()).isNull());
                }
            }
        }
    }
}
