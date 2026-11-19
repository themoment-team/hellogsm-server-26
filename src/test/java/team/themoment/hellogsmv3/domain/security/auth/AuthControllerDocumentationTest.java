package team.themoment.hellogsmv3.domain.security.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import team.themoment.hellogsmv3.global.security.auth.AuthController;
import team.themoment.hellogsmv3.global.security.auth.service.OAuthAuthenticationService;
import tools.jackson.databind.ObjectMapper;

@DisplayName("AuthController 응답 문서 테스트")
class AuthControllerDocumentationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthController controller = new AuthController(mock(OAuthAuthenticationService.class), objectMapper);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("logout 메서드는")
    class Describe_logout {
        @Nested
        @DisplayName("OAuth2 세션으로 요청하면")
        class Context_with_oauth_session {
            @Test
            @DisplayName("문서에 명시한 200 JSON을 직접 쓰고 세션을 무효화한다")
            void it_matches_documented_json() throws Exception {
                var authorities = List.of(new SimpleGrantedAuthority("APPLICANT"));
                var user = new DefaultOAuth2User(authorities, Map.of("id", 1L), "id");
                SecurityContextHolder.getContext()
                        .setAuthentication(new OAuth2AuthenticationToken(user, authorities, "google"));
                var request = new MockHttpServletRequest();
                var session = request.getSession();
                var response = new MockHttpServletResponse();

                controller.logout(request, response);

                ApiResponse documented = Arrays
                        .stream(AuthController.class
                                .getMethod("logout", HttpServletRequest.class, HttpServletResponse.class)
                                .getAnnotation(ApiResponses.class).value())
                        .filter(api -> api.responseCode().equals("200")).findFirst().orElseThrow();
                assertEquals(200, response.getStatus());
                assertTrue(response.getContentType().startsWith(documented.content()[0].mediaType()));
                assertEquals(objectMapper.readTree(documented.content()[0].examples()[0].value()),
                        objectMapper.readTree(response.getContentAsString()));
                assertFalse(objectMapper.readTree(response.getContentAsString()).has("data"));
                assertThrows(IllegalStateException.class, session::getCreationTime);
                assertNull(SecurityContextHolder.getContext().getAuthentication());
            }
        }
    }
}
