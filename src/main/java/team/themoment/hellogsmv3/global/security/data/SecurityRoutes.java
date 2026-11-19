package team.themoment.hellogsmv3.global.security.data;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

import team.themoment.hellogsmv3.domain.member.entity.type.Role;

/**
 * SecurityFilterChain과 OpenAPI가 공유하는 경로별 권한 정책입니다. 등록 순서대로 처음 일치한 규칙을 적용합니다.
 */
public final class SecurityRoutes {
    private static final String[] ALL_AUTHENTICATED = {Role.UNAUTHENTICATED.name(), Role.APPLICANT.name(),
            Role.ADMIN.name(), Role.ROOT.name()};
    private static final String[] ADMIN_ONLY = {Role.ADMIN.name(), Role.ROOT.name()};
    private static final String[] APPLICANT_OR_ROOT = {Role.APPLICANT.name(), Role.ROOT.name()};
    private static final String[] UNAUTHENTICATED_OR_APPLICANT = {Role.UNAUTHENTICATED.name(), Role.APPLICANT.name()};
    private static final String[] APPLICANT_OR_ADMIN_OR_ROOT = {Role.APPLICANT.name(), Role.ADMIN.name(),
            Role.ROOT.name()};

    private static final List<Rule> RULES = List.of(Rule.permitAll(HttpMethod.OPTIONS, "/**/*"),
            Rule.permitAll(HttpMethod.GET, "/date"),
            Rule.permitAll(HttpMethod.DELETE, "/utility/v3/**"),
            Rule.permitAll(HttpMethod.PATCH, "/utility/v3/**"),
            Rule.permitAll(null, "/auth/v3/**"),
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

        static Rule permitAll(HttpMethod method, String path) {
            return new Rule(method, path, null);
        }

        boolean isPermitAll() {
            return authorities == null;
        }
    }

    public static void authorizeRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        for (Rule rule : RULES) {
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl url = rule.method() == null
                    ? registry.requestMatchers(rule.path())
                    : registry.requestMatchers(rule.method(), rule.path());
            if (rule.isPermitAll()) {
                url.permitAll();
            } else {
                url.hasAnyAuthority(rule.authorities());
            }
        }
    }

    /** 일치하는 규칙이 없으면 SecurityConfig의 anyRequest().permitAll()에 따라 공개입니다. */
    public static boolean isPublicRequest(HttpMethod method, String path) {
        return RULES.stream().filter(rule -> rule.matches(method, path)).findFirst().map(Rule::isPermitAll)
                .orElse(true);
    }
}
