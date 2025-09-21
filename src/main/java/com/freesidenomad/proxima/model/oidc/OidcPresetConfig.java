package com.freesidenomad.proxima.model.oidc;

import com.freesidenomad.proxima.model.OidcConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
public class OidcPresetConfig {
    @Builder.Default
    private boolean enabled = false;

    private String subject;
    @Builder.Default
    private Long tokenExpirationSeconds = 3600L;
    @Builder.Default
    private String algorithm = "RS256";
    @Builder.Default
    private String keyId = "default";

    private String email;
    private String name;
    private String preferredUsername;
    @Builder.Default
    private List<String> groups = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> customClaims = new HashMap<>();

    public void setCustomClaims(Map<String, Object> customClaims) {
        this.customClaims = customClaims != null ? customClaims : new HashMap<>();
    }

    @Builder.Default
    private List<String> scopes = Arrays.asList("openid", "profile", "email");

    private String clientId;
    private String redirectUri;

    public OidcPresetConfig(boolean enabled, String subject, Long tokenExpirationSeconds,
                           String algorithm, String keyId, String email, String name,
                           String preferredUsername, List<String> groups,
                           Map<String, Object> customClaims, List<String> scopes,
                           String clientId, String redirectUri) {
        this.enabled = enabled;
        this.subject = subject;
        this.tokenExpirationSeconds = tokenExpirationSeconds;
        this.algorithm = algorithm;
        this.keyId = keyId;
        this.email = email;
        this.name = name;
        this.preferredUsername = preferredUsername;
        this.groups = groups != null ? groups : new ArrayList<>();
        setCustomClaims(customClaims);
        this.scopes = scopes != null ? scopes : Arrays.asList("openid", "profile", "email");
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    public Map<String, Object> toClaims(String issuer, List<String> audience) {
        Map<String, Object> allClaims = new HashMap<>(customClaims != null ? customClaims : new HashMap<>());

        if (email != null) allClaims.put("email", email);
        if (name != null) allClaims.put("name", name);
        if (preferredUsername != null) allClaims.put("preferred_username", preferredUsername);
        if (!groups.isEmpty()) allClaims.put("groups", groups);

        allClaims.put("sub", subject);
        allClaims.put("iss", issuer);
        allClaims.put("aud", audience);
        allClaims.put("exp", Instant.now().plusSeconds(tokenExpirationSeconds).getEpochSecond());
        allClaims.put("iat", Instant.now().getEpochSecond());

        return allClaims;
    }
}