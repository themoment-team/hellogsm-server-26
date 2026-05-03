package team.themoment.hellogsmv3.domain.security.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.security.auth.dto.UserAuthInfo;
import team.themoment.hellogsmv3.global.security.auth.service.OAuthAuthenticationService;
import team.themoment.hellogsmv3.global.security.auth.service.OAuthProviderFactory;
import team.themoment.hellogsmv3.global.security.auth.service.provider.OAuthProvider;
import team.themoment.hellogsmv3.global.security.data.AuthEnvironment;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("OAuthAuthenticationService 클래스의")
class OAuthAuthenticationServiceTest {

    @Mock
    private OAuthProviderFactory oAuthProviderFactory;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AuthEnvironment authEnvironment;

    @Mock
    private OAuthProvider oAuthProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock
    private SecurityContext securityContext;

    private OAuthAuthenticationService oAuthAuthenticationService;

    private static final String ALLOWED_REDIRECT_URI = "http://localhost:3000/oauth/callback";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        oAuthAuthenticationService = new OAuthAuthenticationService(oAuthProviderFactory,
                memberRepository,
                authEnvironment);
        ReflectionTestUtils.setField(oAuthAuthenticationService, "sessionTimeout", Duration.ofSeconds(10800));
        given(authEnvironment.allowedRedirectUris()).willReturn(List.of(ALLOWED_REDIRECT_URI));
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("유효한 provider와 code가 주어지고 기존 회원이 존재하면")
        class Context_with_valid_provider_and_code_and_existing_member {

            private final String provider = "google";
            private final String code = "auth_code_123";
            private final String redirectUri = ALLOWED_REDIRECT_URI;
            private final String email = "s24058@gsm.hs.kr";
            private final AuthReferrerType authReferrerType = AuthReferrerType.GOOGLE;
            private final Long memberId = 1L;

            private Member existingMember;
            private UserAuthInfo userAuthInfo;

            @BeforeEach
            void setUp() {
                existingMember = Member.builder().id(memberId).email(email).role(Role.APPLICANT)
                        .authReferrerType(authReferrerType).build();

                userAuthInfo = new UserAuthInfo(email, provider, authReferrerType);

                given(oAuthProviderFactory.getProvider(provider)).willReturn(oAuthProvider);
                given(oAuthProvider.authenticate(code, redirectUri)).willReturn(userAuthInfo);
                given(memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email))
                        .willReturn(Optional.of(existingMember));
                given(request.getSession(false)).willReturn(session);
                given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                        .willReturn(null);
                given(session.getCreationTime()).willReturn(System.currentTimeMillis());
                given(session.getLastAccessedTime()).willReturn(System.currentTimeMillis());
            }

            @Test
            @DisplayName("OAuth 인증을 완료하고 보안 컨텍스트에 인증 정보를 설정한다")
            void it_completes_oauth_authentication_and_sets_security_context() {
                try (MockedStatic<SecurityContextHolder> mockSecurityContextHolder = mockStatic(
                        SecurityContextHolder.class)) {
                    mockSecurityContextHolder.when(SecurityContextHolder::createEmptyContext)
                            .thenReturn(securityContext);

                    oAuthAuthenticationService.execute(provider, code, redirectUri, request);

                    verify(oAuthProviderFactory).getProvider(provider);
                    verify(oAuthProvider).authenticate(code, redirectUri);
                    verify(memberRepository).findByAuthReferrerTypeAndEmail(authReferrerType, email);
                    verify(memberRepository, never()).save(any(Member.class));

                    ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
                    verify(securityContext).setAuthentication(authCaptor.capture());

                    Authentication authentication = authCaptor.getValue();
                    assertInstanceOf(OAuth2AuthenticationToken.class, authentication);
                    OAuth2AuthenticationToken oAuthToken = (OAuth2AuthenticationToken) authentication;

                    assertEquals(provider, oAuthToken.getAuthorizedClientRegistrationId());
                    assertTrue(oAuthToken.getAuthorities().contains(new SimpleGrantedAuthority("OAUTH2_USER")));
                    assertTrue(oAuthToken.getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_email")));
                    assertTrue(oAuthToken.getAuthorities().contains(new SimpleGrantedAuthority(Role.APPLICANT.name())));

                    DefaultOAuth2User oAuth2User = (DefaultOAuth2User) oAuthToken.getPrincipal();
                    assertEquals(memberId, oAuth2User.getAttribute("id"));
                    assertEquals(Role.APPLICANT, oAuth2User.getAttribute("role"));
                    assertEquals(provider, oAuth2User.getAttribute("provider"));
                    assertEquals(email, oAuth2User.getAttribute("email"));
                    assertNotNull(oAuth2User.getAttribute("last_login_time"));

                    mockSecurityContextHolder.verify(() -> SecurityContextHolder.setContext(securityContext));
                    verify(session).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            securityContext);
                    verify(session).setMaxInactiveInterval(10800);
                }
            }
        }

        @Nested
        @DisplayName("유효한 provider와 code가 주어지고 신규 회원이면")
        class Context_with_valid_provider_and_code_and_new_member {

            private final String provider = "google";
            private final String code = "auth_code_123";
            private final String redirectUri = ALLOWED_REDIRECT_URI;
            private final String email = "s24059@gsm.hs.kr";
            private final AuthReferrerType authReferrerType = AuthReferrerType.GOOGLE;
            private final Long newMemberId = 2L;

            private Member newMember;
            private UserAuthInfo userAuthInfo;

            @BeforeEach
            void setUp() {
                newMember = Member.builder().id(newMemberId).email(email).role(Role.UNAUTHENTICATED)
                        .authReferrerType(authReferrerType).build();

                userAuthInfo = new UserAuthInfo(email, provider, authReferrerType);

                given(oAuthProviderFactory.getProvider(provider)).willReturn(oAuthProvider);
                given(oAuthProvider.authenticate(code, redirectUri)).willReturn(userAuthInfo);
                given(memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email))
                        .willReturn(Optional.empty());
                given(memberRepository.save(any(Member.class))).willReturn(newMember);
                given(request.getSession(false)).willReturn(session);
                given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                        .willReturn(null);
                given(session.getCreationTime()).willReturn(System.currentTimeMillis());
                given(session.getLastAccessedTime()).willReturn(System.currentTimeMillis());
            }

            @Test
            @DisplayName("새로운 회원을 생성하고 OAuth 인증을 완료한다")
            void it_creates_new_member_and_completes_oauth_authentication() {
                try (MockedStatic<SecurityContextHolder> mockSecurityContextHolder = mockStatic(
                        SecurityContextHolder.class)) {
                    mockSecurityContextHolder.when(SecurityContextHolder::createEmptyContext)
                            .thenReturn(securityContext);

                    oAuthAuthenticationService.execute(provider, code, redirectUri, request);

                    verify(oAuthProviderFactory).getProvider(provider);
                    verify(oAuthProvider).authenticate(code, redirectUri);
                    verify(memberRepository).findByAuthReferrerTypeAndEmail(authReferrerType, email);

                    ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
                    verify(memberRepository).save(memberCaptor.capture());

                    Member savedMember = memberCaptor.getValue();
                    assertEquals(email, savedMember.getEmail());
                    assertEquals(authReferrerType, savedMember.getAuthReferrerType());

                    ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
                    verify(securityContext).setAuthentication(authCaptor.capture());

                    Authentication authentication = authCaptor.getValue();
                    assertInstanceOf(OAuth2AuthenticationToken.class, authentication);

                    mockSecurityContextHolder.verify(() -> SecurityContextHolder.setContext(securityContext));
                    verify(session).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            securityContext);
                    verify(session).setMaxInactiveInterval(10800);
                }
            }
        }

        @Nested
        @DisplayName("세션이 무효화된 상태에서 요청이 오면")
        class Context_with_invalidated_session {

            private final String provider = "google";
            private final String code = "auth_code_123";
            private final String redirectUri = ALLOWED_REDIRECT_URI;
            private final String email = "s23020@gsm.hs.kr";
            private final AuthReferrerType authReferrerType = AuthReferrerType.GOOGLE;

            private Member existingMember;
            private UserAuthInfo userAuthInfo;
            private HttpSession newSession;

            @BeforeEach
            void setUp() {
                existingMember = Member.builder().id(1L).email(email).role(Role.APPLICANT)
                        .authReferrerType(authReferrerType).build();

                userAuthInfo = new UserAuthInfo(email, provider, authReferrerType);
                newSession = mock(HttpSession.class);

                given(oAuthProviderFactory.getProvider(provider)).willReturn(oAuthProvider);
                given(oAuthProvider.authenticate(code, redirectUri)).willReturn(userAuthInfo);
                given(memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email))
                        .willReturn(Optional.of(existingMember));
                given(request.getSession(false)).willReturn(session);

                given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                        .willThrow(new IllegalStateException("Session invalidated"));
                given(session.getCreationTime()).willThrow(new IllegalStateException("Session invalidated"));
                given(session.getLastAccessedTime()).willThrow(new IllegalStateException("Session invalidated"));

                given(request.getSession(true)).willReturn(newSession);
            }

            @Test
            @DisplayName("새로운 세션을 생성하고 보안 컨텍스트를 설정한다")
            void it_creates_new_session_and_sets_security_context() {
                try (MockedStatic<SecurityContextHolder> mockSecurityContextHolder = mockStatic(
                        SecurityContextHolder.class)) {
                    mockSecurityContextHolder.when(SecurityContextHolder::createEmptyContext)
                            .thenReturn(securityContext);

                    oAuthAuthenticationService.execute(provider, code, redirectUri, request);

                    verify(session).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                    verify(session).invalidate();

                    verify(request).getSession(true);
                    verify(newSession).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            securityContext);
                    verify(newSession).setMaxInactiveInterval(10800);

                    mockSecurityContextHolder.verify(() -> SecurityContextHolder.setContext(securityContext));
                }
            }
        }

        @Nested
        @DisplayName("세션이 없는 상태에서 요청이 오면")
        class Context_with_no_session {

            private final String provider = "google";
            private final String code = "auth_code_123";
            private final String redirectUri = ALLOWED_REDIRECT_URI;
            private final String email = "s24059@gsm.hs.kr";
            private final AuthReferrerType authReferrerType = AuthReferrerType.GOOGLE;

            private Member existingMember;
            private UserAuthInfo userAuthInfo;
            private HttpSession newSession;

            @BeforeEach
            void setUp() {
                existingMember = Member.builder().id(1L).email(email).role(Role.APPLICANT)
                        .authReferrerType(authReferrerType).build();

                userAuthInfo = new UserAuthInfo(email, provider, authReferrerType);
                newSession = mock(HttpSession.class);

                given(oAuthProviderFactory.getProvider(provider)).willReturn(oAuthProvider);
                given(oAuthProvider.authenticate(code, redirectUri)).willReturn(userAuthInfo);
                given(memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email))
                        .willReturn(Optional.of(existingMember));
                given(request.getSession(false)).willReturn(null);
                given(request.getSession(true)).willReturn(newSession);
            }

            @Test
            @DisplayName("새로운 세션을 생성하고 보안 컨텍스트를 설정한다")
            void it_creates_new_session_and_sets_security_context() {
                try (MockedStatic<SecurityContextHolder> mockSecurityContextHolder = mockStatic(
                        SecurityContextHolder.class)) {
                    mockSecurityContextHolder.when(SecurityContextHolder::createEmptyContext)
                            .thenReturn(securityContext);

                    oAuthAuthenticationService.execute(provider, code, redirectUri, request);

                    verify(request).getSession(false);
                    verify(request).getSession(true);
                    verify(newSession).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            securityContext);
                    verify(newSession).setMaxInactiveInterval(10800);

                    mockSecurityContextHolder.verify(() -> SecurityContextHolder.setContext(securityContext));
                }
            }
        }

        @Nested
        @DisplayName("회원의 role이 null인 경우")
        class Context_with_null_member_role {

            private final String provider = "google";
            private final String code = "auth_code_123";
            private final String redirectUri = ALLOWED_REDIRECT_URI;
            private final String email = "s23009@gsm.hs.kr";
            private final AuthReferrerType authReferrerType = AuthReferrerType.GOOGLE;

            private Member memberWithNullRole;
            private UserAuthInfo userAuthInfo;

            @BeforeEach
            void setUp() {
                memberWithNullRole = Member.builder().id(1L).email(email).role(null).authReferrerType(authReferrerType)
                        .build();

                userAuthInfo = new UserAuthInfo(email, provider, authReferrerType);

                given(oAuthProviderFactory.getProvider(provider)).willReturn(oAuthProvider);
                given(oAuthProvider.authenticate(code, redirectUri)).willReturn(userAuthInfo);
                given(memberRepository.findByAuthReferrerTypeAndEmail(authReferrerType, email))
                        .willReturn(Optional.of(memberWithNullRole));
                given(request.getSession(false)).willReturn(session);
                given(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                        .willReturn(null);
                given(session.getCreationTime()).willReturn(System.currentTimeMillis());
                given(session.getLastAccessedTime()).willReturn(System.currentTimeMillis());
            }

            @Test
            @DisplayName("기본 권한 UNAUTHENTICATED를 설정한다")
            void it_sets_default_unauthenticated_authority() {
                try (MockedStatic<SecurityContextHolder> mockSecurityContextHolder = mockStatic(
                        SecurityContextHolder.class)) {
                    mockSecurityContextHolder.when(SecurityContextHolder::createEmptyContext)
                            .thenReturn(securityContext);

                    oAuthAuthenticationService.execute(provider, code, redirectUri, request);

                    ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
                    verify(securityContext).setAuthentication(authCaptor.capture());

                    Authentication authentication = authCaptor.getValue();
                    assertInstanceOf(OAuth2AuthenticationToken.class, authentication);
                    OAuth2AuthenticationToken oAuthToken = (OAuth2AuthenticationToken) authentication;

                    assertTrue(oAuthToken.getAuthorities()
                            .contains(new SimpleGrantedAuthority(Role.UNAUTHENTICATED.name())));

                    verify(session).setMaxInactiveInterval(10800);
                    mockSecurityContextHolder.verify(() -> SecurityContextHolder.setContext(securityContext));
                }
            }
        }

        @Nested
        @DisplayName("허용 목록에 없는 redirect_uri가 주어진 경우")
        class Context_with_disallowed_redirect_uri {

            private final String provider = "google";
            private final String code = "auth_code_123";
            private final String redirectUri = "https://malicious.example.com/callback";

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                assertThrows(ExpectedException.class,
                        () -> oAuthAuthenticationService.execute(provider, code, redirectUri, request));

                verify(oAuthProviderFactory, never()).getProvider(any());
            }
        }
    }
}
