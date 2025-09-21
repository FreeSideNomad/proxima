package com.freesidenomad.proxima.model.oidc;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationCodeTest {

    @Test
    void shouldCreateAuthorizationCodeWithValidParameters() {
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";
        String scope = "openid profile";
        String state = "test-state";
        String nonce = "test-nonce";
        String subject = "test@example.com";

        AuthorizationCode code = AuthorizationCode.create(
            clientId, redirectUri, scope, state, nonce, subject);

        assertNotNull(code);
        assertNotNull(code.getCode());
        assertFalse(code.getCode().isEmpty());
        assertEquals(clientId, code.getClientId());
        assertEquals(redirectUri, code.getRedirectUri());
        assertEquals(scope, code.getScope());
        assertEquals(state, code.getState());
        assertEquals(nonce, code.getNonce());
        assertEquals(subject, code.getSubject());
        assertNotNull(code.getCreatedAt());
        assertNotNull(code.getExpiresAt());
        assertFalse(code.isUsed());
    }

    @Test
    void shouldGenerateUniqueAuthorizationCodes() {
        AuthorizationCode code1 = AuthorizationCode.create(
            "client1", "http://localhost/callback", "openid", "state1", "nonce1", "user1");
        AuthorizationCode code2 = AuthorizationCode.create(
            "client2", "http://localhost/callback", "openid", "state2", "nonce2", "user2");

        assertNotEquals(code1.getCode(), code2.getCode());
    }

    @Test
    void shouldSetExpirationTime() {
        AuthorizationCode code = AuthorizationCode.create(
            "test-client", "http://localhost/callback", "openid", "state", "nonce", "user");

        assertNotNull(code.getExpiresAt());
        assertTrue(code.getExpiresAt().isAfter(Instant.now()));
        assertTrue(code.getExpiresAt().isBefore(Instant.now().plusSeconds(700))); // Should be ~10 minutes
    }

    @Test
    void shouldBeValidWhenNotExpiredAndNotUsed() {
        AuthorizationCode code = AuthorizationCode.create(
            "test-client", "http://localhost/callback", "openid", "state", "nonce", "user");

        assertTrue(code.isValid());
        assertFalse(code.isExpired());
        assertFalse(code.isUsed());
    }

    @Test
    void shouldBeInvalidWhenUsed() {
        AuthorizationCode code = AuthorizationCode.create(
            "test-client", "http://localhost/callback", "openid", "state", "nonce", "user");

        code.markAsUsed();

        assertFalse(code.isValid());
        assertTrue(code.isUsed());
    }

    @Test
    void shouldBeInvalidWhenExpired() {
        AuthorizationCode code = AuthorizationCode.builder()
            .code("test-code")
            .clientId("test-client")
            .redirectUri("http://localhost/callback")
            .scope("openid")
            .state("state")
            .nonce("nonce")
            .subject("user")
            .expiresAt(Instant.now().minusSeconds(1)) // Already expired
            .createdAt(Instant.now().minusSeconds(60))
            .used(false)
            .build();

        assertFalse(code.isValid());
        assertTrue(code.isExpired());
        assertFalse(code.isUsed());
    }

    @Test
    void shouldGenerateCodeWithoutDashes() {
        AuthorizationCode code = AuthorizationCode.create(
            "test-client", "http://localhost/callback", "openid", "state", "nonce", "user");

        assertFalse(code.getCode().contains("-"));
        assertTrue(code.getCode().length() > 20); // Should be reasonable length
    }

    @Test
    void shouldHandleNullOptionalParameters() {
        AuthorizationCode code = AuthorizationCode.create(
            "test-client", "http://localhost/callback", "openid", null, null, "user");

        assertNotNull(code);
        assertNull(code.getState());
        assertNull(code.getNonce());
        assertEquals("test-client", code.getClientId());
        assertEquals("user", code.getSubject());
    }
}