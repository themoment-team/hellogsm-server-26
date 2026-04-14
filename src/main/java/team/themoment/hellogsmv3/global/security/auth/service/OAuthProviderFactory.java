package team.themoment.hellogsmv3.global.security.auth.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.auth.service.provider.OAuthProvider;

@Component
public class OAuthProviderFactory {

    private final Map<String, OAuthProvider> providers;

    public OAuthProviderFactory(List<OAuthProvider> oAuthProviders) {
        this.providers = oAuthProviders.stream()
                .collect(Collectors.toMap(provider -> provider.getProviderName().toLowerCase(), Function.identity()));
    }

    public OAuthProvider getProvider(String providerName) {
        OAuthProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            throw new ExpectedException("지원하지 않는 OAuth Provider입니다: " + providerName, HttpStatus.BAD_REQUEST);
        }
        return provider;
    }
}
