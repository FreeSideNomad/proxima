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
    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private Long expiresIn;
    private Instant expiresAt;
    private String scope;

    public OidcTokens(String accessToken, String tokenType, String refreshToken,
                     Long expiresIn, Instant expiresAt, String scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.expiresAt = expiresAt;
        this.scope = scope;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return accessToken != null && !isExpired();
    }
}