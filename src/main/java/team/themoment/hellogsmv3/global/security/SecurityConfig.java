package team.themoment.hellogsmv3.global.security;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.global.security.data.AuthEnvironment;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;
import team.themoment.hellogsmv3.global.security.filter.TimeBasedFilter;
import team.themoment.hellogsmv3.global.security.handler.CustomAccessDeniedHandler;
import team.themoment.hellogsmv3.global.security.handler.CustomAuthenticationEntryPoint;
import team.themoment.sdk.logging.LoggingFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final ScheduleEnvironment scheduleEnv;
    private final AuthEnvironment authEnv;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final LoggingFilter loggingFilter;

    private static final String[] ALL_AUTHENTICATED = {Role.UNAUTHENTICATED.name(), Role.APPLICANT.name(),
            Role.ADMIN.name(), Role.ROOT.name()};
    private static final String[] ADMIN_ONLY = {Role.ADMIN.name(), Role.ROOT.name()};
    private static final String[] APPLICANT_OR_ROOT = {Role.APPLICANT.name(), Role.ROOT.name()};
    private static final String[] UNAUTHENTICATED_OR_APPLICANT = {Role.UNAUTHENTICATED.name(), Role.APPLICANT.name()};

    @Bean
    public Filter timeBasedFilter() {
        LocalDateTime oneseoSubmissionStart = scheduleEnv.oneseoSubmissionStart();
        LocalDateTime oneseoSubmissionEnd = scheduleEnv.oneseoSubmissionEnd();
        LocalDateTime interview = scheduleEnv.interview();
        LocalDateTime finalResultsAnnouncement = scheduleEnv.finalResultsAnnouncement();

        return new TimeBasedFilter()
                .addFilter(HttpMethod.POST, "/oneseo/v3/temp-storage", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.POST, "/oneseo/v3/oneseo/me", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.PUT, "/oneseo/v3/oneseo/{memberId}", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.POST, "/oneseo/v3/image", oneseoSubmissionStart, oneseoSubmissionEnd)
                .addFilter(HttpMethod.POST, "/oneseo/v3/excel", interview, finalResultsAnnouncement);
    }

    @Configuration
    @EnableWebSecurity
    public class LocalSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

            basicSetting(http);
            cors(http);
            exceptionHandling(http);
            authorizeHttpRequests(http);
            addLoggingFilter(http);

            return http.build();
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(authEnv.allowedOrigins());

        configuration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));

        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void basicSetting(HttpSecurity http) throws Exception {
        http.formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession());
    }

    private void cors(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(corsConfigurationSource()));
    }

    private void exceptionHandling(HttpSecurity http) throws Exception {
        http.exceptionHandling(handling -> handling.accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint));
    }

    private void authorizeHttpRequests(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(req -> {
            commonRequests(req);
            authRequests(req);
            memberRequests(req);
            oneseoRequests(req);
            operationRequests(req);
            testResultRequests(req);
            req.anyRequest().permitAll();
        });
    }

    private void commonRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        req.requestMatchers(HttpMethod.OPTIONS, "/**/*").permitAll().requestMatchers(HttpMethod.GET, "/date")
                .permitAll().requestMatchers(HttpMethod.DELETE, "/utility/v3/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/utility/v3/**").permitAll();
    }

    private void authRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        req.requestMatchers("/auth/v3/**").permitAll();
    }

    private void memberRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        req.requestMatchers(HttpMethod.GET, "/member/v3/member/me").hasAnyAuthority(ALL_AUTHENTICATED)
                .requestMatchers(HttpMethod.GET, "/member/v3/member/{memberId}").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.POST, "/member/v3/member/me/send-code", "/member/v3/member/me/auth-code")
                .hasAnyAuthority(ALL_AUTHENTICATED)
                .requestMatchers(HttpMethod.POST, "/member/v3/member/me/send-code-test")
                .hasAnyAuthority(Role.ROOT.name()).requestMatchers(HttpMethod.POST, "/member/v3/member/me")
                .hasAnyAuthority(ALL_AUTHENTICATED).requestMatchers(HttpMethod.GET, "/member/v3/auth-info/me")
                .hasAnyAuthority(ALL_AUTHENTICATED).requestMatchers(HttpMethod.GET, "/member/v3/auth-info/{memberId}")
                .hasAnyAuthority(ADMIN_ONLY).requestMatchers(HttpMethod.GET, "/member/v3/first-test-result/me")
                .hasAnyAuthority(APPLICANT_OR_ROOT).requestMatchers(HttpMethod.GET, "/member/v3/second-test-result/me")
                .hasAnyAuthority(APPLICANT_OR_ROOT).requestMatchers(HttpMethod.GET, "/member/v3/check-duplicate")
                .hasAnyAuthority(Role.UNAUTHENTICATED.name(), Role.ROOT.name());
    }

    private void oneseoRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        req.requestMatchers("/oneseo/v3/oneseo/me").hasAnyAuthority(APPLICANT_OR_ROOT)
                .requestMatchers("/oneseo/v3/oneseo/{memberId}").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.PATCH, "/oneseo/v3/arrived-status/{memberId}").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.PATCH, "/oneseo/v3/competency-score/{memberId}").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.PATCH, "/oneseo/v3/interview-score/{memberId}").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.POST, "/oneseo/v3/image")
                .hasAnyAuthority(Role.APPLICANT.name(), Role.ADMIN.name(), Role.ROOT.name())
                .requestMatchers(HttpMethod.DELETE, "/oneseo/v3/oneseo/me").hasAnyAuthority(APPLICANT_OR_ROOT)
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/oneseo/search").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.PUT, "/oneseo/v3/final-submit").hasAnyAuthority(Role.APPLICANT.name())
                .requestMatchers(HttpMethod.POST, "/oneseo/v3/excel").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/excel").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/admission-tickets").hasAnyAuthority(ADMIN_ONLY)
                .requestMatchers(HttpMethod.GET, "/oneseo/v3/editability").hasAnyAuthority(ADMIN_ONLY);
    }

    private void operationRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        req.requestMatchers("/operation/**").hasAnyAuthority(ADMIN_ONLY);
    }

    private void testResultRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry req) {
        req.requestMatchers("/test-result/v3/auth-code").hasAnyAuthority(UNAUTHENTICATED_OR_APPLICANT)
                .requestMatchers("/test-result/v3/send-code").hasAnyAuthority(UNAUTHENTICATED_OR_APPLICANT)
                .requestMatchers("/test-result/v3/my/**").hasAnyAuthority(UNAUTHENTICATED_OR_APPLICANT);
    }

    private void addLoggingFilter(HttpSecurity http) {
        http.addFilterBefore(loggingFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
