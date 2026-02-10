package team.themoment.hellogsmv3.global.security.auth;

import java.util.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;

@Component
public class AuthenticatedUserManager {
    public Role setRole(HttpServletRequest req, Role role) {
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();

        Map<String, Object> newAttributes = new HashMap<>(oAuth2User.getAttributes());
        newAttributes.put("role", role.name());

        Collection<GrantedAuthority> newAuthorities = new ArrayList<>(oAuth2User.getAuthorities());

        newAuthorities.remove(new SimpleGrantedAuthority(Role.UNAUTHENTICATED.name()));
        newAuthorities.add(new SimpleGrantedAuthority(role.name()));

        OAuth2User newOAuth2User = new DefaultOAuth2User(newAuthorities, newAttributes, "id");

        OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(newOAuth2User,
                newAuthorities,
                oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(newAuth);
        HttpSession session = req.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        return role;
    }
}
