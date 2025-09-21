package com.freesidenomad.proxima.model.oidc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCode {
    private String code;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String nonce;
    private String subject;
    private Instant expiresAt;
    private Instant createdAt;
    private boolean used;

    public static AuthorizationCode create(String clientId, String redirectUri, String scope,
                                         String state, String nonce, String subject) {
        return AuthorizationCode.builder()
            .code(generateCode())
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scope(scope)
            .state(state)
            .nonce(nonce)
            .subject(subject)
            .expiresAt(Instant.now().plusSeconds(600)) // 10 minutes
            .createdAt(Instant.now())
            .used(false)
            .build();
    }

    private static String generateCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
    }
}