package com.freesidenomad.proxima.model.oidc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
public class OidcTokens {
    private String idToken;
    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private Long expiresIn;
    private Instant expiresAt;
    private String scope;
    private Instant createdAt;

    public OidcTokens(String idToken, String accessToken, String tokenType,
                     String refreshToken, Long expiresIn, Instant expiresAt,
                     String scope, Instant createdAt) {
        this.idToken = idToken;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.scope = scope;
        this.createdAt = createdAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return accessToken != null && !isExpired();
    }
}