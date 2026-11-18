package team.themoment.hellogsmv3.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import team.themoment.hellogsmv3.global.security.data.SecurityRoutes;

@DisplayName("SecurityRoutes 권한 정책 테스트")
class SecurityRoutesTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(Fixture.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Nested
    @DisplayName("입학등록 동의서 수정 요청은")
    class Describe_entranceIntention {
        @Nested
        @DisplayName("인증 또는 관리자 권한이 없는 경우")
        class Context_without_authority {
            @Test
            @DisplayName("익명은 401 지원자는 403으로 차단한다")
            void it_rejects_anonymous_and_applicant() throws Exception {
                mvc.perform(patch("/oneseo/v3/entrance-intention/123")).andExpect(status().isUnauthorized());
                mvc.perform(patch("/oneseo/v3/entrance-intention/123").session(session("APPLICANT")))
                        .andExpect(status().isForbidden());
                mvc.perform(patch("/oneseo/v3/entrance-intention/123").session(session("UNAUTHENTICATED")))
                        .andExpect(status().isForbidden());
            }
        }
        @Nested
        @DisplayName("ADMIN 또는 ROOT인 경우")
        class Context_admin {
            @Test
            @DisplayName("요청을 허용한다")
            void it_allows_admin_and_root() throws Exception {
                for (String role : List.of("ADMIN", "ROOT")) {
                    mvc.perform(patch("/oneseo/v3/entrance-intention/123").session(session(role)))
                            .andExpect(status().isOk());
                }
            }
        }
    }

    @Nested
    @DisplayName("isPublicRequest 메서드는")
    class Describe_isPublicRequest {
        @Test
        @DisplayName("메서드 경로 와일드카드와 OPTIONS 공개 정책을 일치시킨다")
        void it_matches_method_path_and_options() {
            assertThat(SecurityRoutes.isPublicRequest(HttpMethod.POST, "/oneseo/v3/extraction/middle-school"))
                    .isFalse();
            assertThat(SecurityRoutes.isPublicRequest(HttpMethod.GET, "/oneseo/v3/extraction/middle-school")).isTrue();
            assertThat(SecurityRoutes.isPublicRequest(HttpMethod.OPTIONS, "/member/v3/member/me")).isTrue();
            assertThat(SecurityRoutes.isPublicRequest(HttpMethod.GET, "/auth/v3/logout")).isTrue();
            assertThat(SecurityRoutes.isPublicRequest(HttpMethod.DELETE, "/oneseo/v3/oneseo/123")).isFalse();
        }
    }

    private MockHttpSession session(String role) {
        var authentication = UsernamePasswordAuthenticationToken
                .authenticated("user", "", List.of(new SimpleGrantedAuthority(role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class Fixture {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(registry -> {
                SecurityRoutes.authorizeProtectedRequests(registry);
                registry.anyRequest().permitAll();
            }).exceptionHandling(
                    handling -> handling.authenticationEntryPoint((request, response, error) -> response.setStatus(401))
                            .accessDeniedHandler((request, response, error) -> response.setStatus(403)));
            return http.build();
        }
        @Bean
        Endpoint endpoint() {
            return new Endpoint();
        }
    }

    @RestController
    static class Endpoint {
        @PatchMapping("/oneseo/v3/entrance-intention/{memberId}")
        public String modify() {
            return "ok";
        }
    }
}
