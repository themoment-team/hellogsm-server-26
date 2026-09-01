package team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfoResDto(@JsonProperty("id") String id, @JsonProperty("email") String email,
        @JsonProperty("verified_email") Boolean verifiedEmail, @JsonProperty("name") String name,
        @JsonProperty("given_name") String givenName, @JsonProperty("family_name") String familyName,
        @JsonProperty("picture") String picture, @JsonProperty("locale") String locale) {
}
