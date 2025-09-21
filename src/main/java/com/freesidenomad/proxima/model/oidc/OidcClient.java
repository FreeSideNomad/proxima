package com.freesidenomad.proxima.model.oidc;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Model representing an OIDC client configuration
 */
@Data
@Builder
public class OidcClient {
    private String clientId;
    private String clientName;
    private String description;
    private List<String> redirectUris;
    private List<String> allowedScopes;
    private boolean enabled;
    private Instant createdAt;
    private Instant modifiedAt;
    private String createdBy;

    // Client metadata
    private String applicationType; // web, native, etc.
    private String tokenEndpointAuthMethod; // client_secret_basic, client_secret_post, none
    private List<String> grantTypes; // authorization_code, refresh_token, etc.
    private List<String> responseTypes; // code, token, id_token

    // Security settings
    private boolean requirePkce;
    private boolean allowPlainCodeChallenge;
    private Integer accessTokenLifetime;
    private Integer idTokenLifetime;
    private Integer refreshTokenLifetime;

    public boolean isValid() {
        return clientId != null && !clientId.trim().isEmpty() &&
               clientName != null && !clientName.trim().isEmpty() &&
               redirectUris != null && !redirectUris.isEmpty() &&
               enabled;
    }

    public static OidcClient create(String clientId, String clientName, String description,
                                   List<String> redirectUris, List<String> allowedScopes) {
        return OidcClient.builder()
            .clientId(clientId)
            .clientName(clientName)
            .description(description)
            .redirectUris(redirectUris)
            .allowedScopes(allowedScopes)
            .enabled(true)
            .createdAt(Instant.now())
            .modifiedAt(Instant.now())
            .applicationType("web")
            .tokenEndpointAuthMethod("none")
            .grantTypes(List.of("authorization_code"))
            .responseTypes(List.of("code"))
            .requirePkce(false)
            .allowPlainCodeChallenge(false)
            .accessTokenLifetime(3600)
            .idTokenLifetime(3600)
            .refreshTokenLifetime(86400)
            .build();
    }
}