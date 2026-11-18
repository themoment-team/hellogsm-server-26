package team.themoment.hellogsmv3.global.security.data;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

import team.themoment.hellogsmv3.domain.member.entity.type.Role;

/** SecurityFilterChain과 OpenAPI가 공유하는 보호 경로 및 권한 정책입니다. */
public final class SecurityRoutes {
    private static final String[] ALL_AUTHENTICATED = {Role.UNAUTHENTICATED.name(), Role.APPLICANT.name(),
            Role.ADMIN.name(), Role.ROOT.name()};
    private static final String[] ADMIN_ONLY = {Role.ADMIN.name(), Role.ROOT.name()};
    private static final String[] APPLICANT_OR_ROOT = {Role.APPLICANT.name(), Role.ROOT.name()};
    private static final String[] UNAUTHENTICATED_OR_APPLICANT = {Role.UNAUTHENTICATED.name(), Role.APPLICANT.name()};
    private static final String[] APPLICANT_OR_ADMIN_OR_ROOT = {Role.APPLICANT.name(), Role.ADMIN.name(),
            Role.ROOT.name()};

    private static final List<Rule> PROTECTED_RULES = List.of(
            new Rule(HttpMethod.GET, "/member/v3/member/me", ALL_AUTHENTICATED),
            new Rule(HttpMethod.GET, "/member/v3/member/{memberId}", ADMIN_ONLY),
            new Rule(HttpMethod.POST, "/member/v3/member/me/send-code", ALL_AUTHENTICATED),
            new Rule(HttpMethod.POST, "/member/v3/member/me/auth-code", ALL_AUTHENTICATED),
            new Rule(HttpMethod.POST, "/member/v3/member/me/send-code-test", new String[]{Role.ROOT.name()}),
            new Rule(HttpMethod.POST, "/member/v3/member/me", ALL_AUTHENTICATED),
            new Rule(HttpMethod.GET, "/member/v3/auth-info/me", ALL_AUTHENTICATED),
            new Rule(HttpMethod.GET, "/member/v3/auth-info/{memberId}", ADMIN_ONLY),
            new Rule(HttpMethod.GET, "/member/v3/first-test-result/me", APPLICANT_OR_ROOT),
            new Rule(HttpMethod.GET, "/member/v3/second-test-result/me", APPLICANT_OR_ROOT),
            new Rule(HttpMethod.GET,
                    "/member/v3/check-duplicate",
                    new String[]{Role.UNAUTHENTICATED.name(), Role.ROOT.name()}),
            new Rule(HttpMethod.GET, "/oneseo/v3/oneseo/me", APPLICANT_OR_ADMIN_OR_ROOT),
            new Rule(HttpMethod.POST, "/oneseo/v3/oneseo/me", APPLICANT_OR_ROOT),
            new Rule(HttpMethod.PUT, "/oneseo/v3/oneseo/me", APPLICANT_OR_ROOT),
            new Rule(HttpMethod.GET, "/oneseo/v3/oneseo/search", ADMIN_ONLY),
            new Rule(null, "/oneseo/v3/oneseo/{memberId}", ADMIN_ONLY),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/arrived-status/{memberId}", ADMIN_ONLY),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/competency-score/{memberId}", ADMIN_ONLY),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/interview-score/{memberId}", ADMIN_ONLY),
            new Rule(HttpMethod.POST,
                    "/oneseo/v3/image",
                    new String[]{Role.APPLICANT.name(), Role.ADMIN.name(), Role.ROOT.name()}),
            new Rule(HttpMethod.PUT, "/oneseo/v3/final-submit", new String[]{Role.APPLICANT.name()}),
            new Rule(HttpMethod.POST, "/oneseo/v3/excel", ADMIN_ONLY),
            new Rule(HttpMethod.GET, "/oneseo/v3/excel", ADMIN_ONLY),
            new Rule(HttpMethod.GET, "/oneseo/v3/admission-tickets", ADMIN_ONLY),
            new Rule(HttpMethod.GET, "/oneseo/v3/editability", ALL_AUTHENTICATED),
            new Rule(HttpMethod.POST, "/oneseo/v3/oneseo/me/request", APPLICANT_OR_ROOT),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/oneseo/{memberId}/approval", ADMIN_ONLY),
            new Rule(HttpMethod.POST, "/oneseo/v3/extraction/**", APPLICANT_OR_ADMIN_OR_ROOT),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/personal-info/me", APPLICANT_OR_ROOT),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/personal-info/{memberId}", ADMIN_ONLY),
            new Rule(null, "/operation/**", ADMIN_ONLY),
            new Rule(null, "/test-result/v3/auth-code", UNAUTHENTICATED_OR_APPLICANT),
            new Rule(null, "/test-result/v3/send-code", UNAUTHENTICATED_OR_APPLICANT),
            new Rule(null, "/test-result/v3/my/**", UNAUTHENTICATED_OR_APPLICANT),
            new Rule(HttpMethod.PATCH, "/oneseo/v3/entrance-intention/{memberId}", ADMIN_ONLY));

    private SecurityRoutes() {
    }

    private record Rule(HttpMethod method, String path, String[] authorities) {
        boolean matches(HttpMethod requestMethod, String requestPath) {
            return (method == null || method.equals(requestMethod))
                    && PathPatternParser.defaultInstance.parse(path).matches(PathContainer.parsePath(requestPath));
        }
    }

    public static void authorizeProtectedRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        for (Rule rule : PROTECTED_RULES) {
            if (rule.method() == null) {
                registry.requestMatchers(rule.path()).hasAnyAuthority(rule.authorities());
            } else {
                registry.requestMatchers(rule.method(), rule.path()).hasAnyAuthority(rule.authorities());
            }
        }
    }

    public static boolean isPublicRequest(HttpMethod method, String path) {
        // OPTIONS는 commonRequests에서 보호 경로보다 먼저 permitAll로 처리됩니다.
        return HttpMethod.OPTIONS.equals(method)
                || PROTECTED_RULES.stream().noneMatch(rule -> rule.matches(method, path));
    }
}
