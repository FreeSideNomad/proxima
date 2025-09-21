package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.oidc.AuthorizationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationCodeServiceTest {

    private AuthorizationCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationCodeServiceImpl();
    }

    @Test
    void shouldGenerateAuthorizationCode() {
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";
        String scope = "openid profile";
        String state = "test-state";
        String nonce = "test-nonce";
        String subject = "test@example.com";

        AuthorizationCode code = service.generateAuthorizationCode(
            clientId, redirectUri, scope, state, nonce, subject);

        assertNotNull(code);
        assertEquals(clientId, code.getClientId());
        assertEquals(redirectUri, code.getRedirectUri());
        assertEquals(scope, code.getScope());
        assertEquals(state, code.getState());
        assertEquals(nonce, code.getNonce());
        assertEquals(subject, code.getSubject());
        assertTrue(code.isValid());
    }

    @Test
    void shouldGenerateUniqueAuthorizationCodes() {
        AuthorizationCode code1 = service.generateAuthorizationCode(
            "client1", "http://localhost/callback", "openid", "state1", "nonce1", "user1");
        AuthorizationCode code2 = service.generateAuthorizationCode(
            "client2", "http://localhost/callback", "openid", "state2", "nonce2", "user2");

        assertNotEquals(code1.getCode(), code2.getCode());
    }

    @Test
    void shouldValidateAndConsumeCode() {
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";
        String scope = "openid profile";
        String state = "test-state";
        String nonce = "test-nonce";
        String subject = "test@example.com";

        // Generate code
        AuthorizationCode originalCode = service.generateAuthorizationCode(
            clientId, redirectUri, scope, state, nonce, subject);

        // Validate and consume
        AuthorizationCode consumedCode = service.validateAndConsumeCode(
            originalCode.getCode(), clientId, redirectUri);

        assertNotNull(consumedCode);
        assertEquals(originalCode.getCode(), consumedCode.getCode());
        assertEquals(clientId, consumedCode.getClientId());
        assertEquals(redirectUri, consumedCode.getRedirectUri());
        assertTrue(consumedCode.isUsed());
    }

    @Test
    void shouldThrowExceptionForInvalidCode() {
        String invalidCode = "invalid-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.validateAndConsumeCode(invalidCode, clientId, redirectUri)
        );

        assertEquals("Invalid authorization code", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionForClientIdMismatch() {
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";

        // Generate code with one client ID
        AuthorizationCode code = service.generateAuthorizationCode(
            clientId, redirectUri, "openid", "state", "nonce", "user");

        // Try to validate with different client ID
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.validateAndConsumeCode(code.getCode(), "wrong-client", redirectUri)
        );

        assertEquals("Client ID mismatch", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionForRedirectUriMismatch() {
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";

        // Generate code with one redirect URI
        AuthorizationCode code = service.generateAuthorizationCode(
            clientId, redirectUri, "openid", "state", "nonce", "user");

        // Try to validate with different redirect URI
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.validateAndConsumeCode(code.getCode(), clientId, "http://evil.com/callback")
        );

        assertEquals("Redirect URI mismatch", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionForAlreadyUsedCode() {
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";

        // Generate and consume code once
        AuthorizationCode code = service.generateAuthorizationCode(
            clientId, redirectUri, "openid", "state", "nonce", "user");
        service.validateAndConsumeCode(code.getCode(), clientId, redirectUri);

        // Try to use the same code again
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.validateAndConsumeCode(code.getCode(), clientId, redirectUri)
        );

        assertEquals("Invalid authorization code", exception.getMessage());
    }

    @Test
    void shouldGetStats() {
        // Initially no codes
        AuthorizationCodeService.AuthorizationCodeStats stats = service.getStats();
        assertEquals(0, stats.activeCodes);
        assertEquals(0, stats.totalGenerated);

        // Generate some codes
        service.generateAuthorizationCode("client1", "http://localhost/callback1", "openid", "state1", "nonce1", "user1");
        service.generateAuthorizationCode("client2", "http://localhost/callback2", "openid", "state2", "nonce2", "user2");

        stats = service.getStats();
        assertEquals(2, stats.activeCodes);
        assertEquals(2, stats.totalGenerated);

        // Consume one code
        AuthorizationCode code = service.generateAuthorizationCode("client3", "http://localhost/callback3", "openid", "state3", "nonce3", "user3");
        service.validateAndConsumeCode(code.getCode(), "client3", "http://localhost/callback3");

        stats = service.getStats();
        assertEquals(2, stats.activeCodes); // Two still active
        assertEquals(3, stats.totalGenerated); // Three total generated
    }

    @Test
    void shouldCleanupExpiredCodes() {
        // This test would require manipulating time or using mock objects
        // For now, we just verify the method exists and doesn't throw
        assertDoesNotThrow(() -> service.cleanupExpiredCodes());
    }
}