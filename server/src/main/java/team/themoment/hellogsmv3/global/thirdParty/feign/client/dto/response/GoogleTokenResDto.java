package team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResDto(@JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType, @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("refresh_token") String refreshToken, @JsonProperty("scope") String scope,
        @JsonProperty("id_token") String idToken) {
}
