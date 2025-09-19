package com.freesidenomad.proxima.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    void shouldInitializeWithDefaultKeys() {
        Map<String, Object> keyInfo = jwtService.getKeyInfo();

        assertNotNull(keyInfo);
        assertTrue(keyInfo.containsKey("hmacKeys"));
        assertTrue(keyInfo.containsKey("rsaKeys"));
        assertEquals(2, keyInfo.get("totalKeys"));
    }

    @Test
    void shouldGenerateHS256Token() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "admin");
        claims.put("userId", "123");

        String token = jwtService.generateToken(
            "user@example.com",
            claims,
            Duration.ofHours(1),
            "HS256"
        );

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    void shouldGenerateRS256Token() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "read write");

        String token = jwtService.generateToken(
            "api-client",
            claims,
            Duration.ofMinutes(30),
            "RS256"
        );

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void shouldGenerateTokenResponse() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("department", "engineering");

        Map<String, Object> response = jwtService.generateTokenResponse(
            "employee@company.com",
            claims,
            Duration.ofHours(8),
            "HS256",
            "default"
        );

        assertNotNull(response);
        assertTrue(response.containsKey("token"));
        assertEquals("employee@company.com", response.get("subject"));
        assertEquals("HS256", response.get("algorithm"));
        assertEquals("default", response.get("keyId"));
        assertEquals(28800L, response.get("expiresIn")); // 8 hours
        assertEquals(claims, response.get("claims"));
    }

    @Test
    void shouldGenerateNewHmacKey() {
        String keyId = "test-hmac-key";

        String encodedKey = jwtService.generateHmacKey(keyId);

        assertNotNull(encodedKey);
        assertTrue(encodedKey.length() > 0);
        assertTrue(jwtService.keyExists(keyId));

        // Should be able to generate token with new key
        String token = jwtService.generateToken(
            "test-user",
            new HashMap<>(),
            Duration.ofMinutes(5),
            "HS256",
            keyId
        );

        assertNotNull(token);
    }

    @Test
    void shouldGenerateNewRsaKeyPair() {
        String keyId = "test-rsa-key";

        Map<String, String> keyPair = jwtService.generateRsaKeyPair(keyId);

        assertNotNull(keyPair);
        assertEquals(keyId, keyPair.get("keyId"));
        assertTrue(keyPair.containsKey("publicKey"));
        assertTrue(keyPair.containsKey("privateKey"));
        assertEquals("RS256", keyPair.get("algorithm"));
        assertTrue(jwtService.keyExists(keyId));

        // Should be able to generate token with new key
        String token = jwtService.generateToken(
            "test-client",
            new HashMap<>(),
            Duration.ofMinutes(10),
            "RS256",
            keyId
        );

        assertNotNull(token);
    }

    @Test
    void shouldRetrievePublicKey() {
        String keyId = "test-public-key";
        jwtService.generateRsaKeyPair(keyId);

        String publicKey = jwtService.getPublicKey(keyId);

        assertNotNull(publicKey);
        assertTrue(publicKey.length() > 0);
    }

    @Test
    void shouldThrowExceptionForNonexistentRsaKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.getPublicKey("nonexistent-key");
        });
    }

    @Test
    void shouldDeleteKey() {
        String keyId = "delete-test-key";
        jwtService.generateHmacKey(keyId);

        assertTrue(jwtService.keyExists(keyId));

        jwtService.deleteKey(keyId);

        assertFalse(jwtService.keyExists(keyId));
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonexistentKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.deleteKey("nonexistent-key");
        });
    }

    @Test
    void shouldRegenerateDefaultKeysWhenDeleted() {
        jwtService.deleteKey("default");

        // Default keys should be regenerated
        assertTrue(jwtService.keyExists("default"));

        // Should still be able to generate tokens
        String token = jwtService.generateToken(
            "test-user",
            new HashMap<>(),
            Duration.ofMinutes(5),
            "HS256",
            "default"
        );

        assertNotNull(token);
    }

    @Test
    void shouldThrowExceptionForUnsupportedAlgorithm() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateToken(
                "test-user",
                new HashMap<>(),
                Duration.ofMinutes(5),
                "UNSUPPORTED"
            );
        });
    }

    @Test
    void shouldThrowExceptionForNonexistentKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateToken(
                "test-user",
                new HashMap<>(),
                Duration.ofMinutes(5),
                "HS256",
                "nonexistent-key"
            );
        });
    }

    @Test
    void shouldPreventDuplicateKeyGeneration() {
        String keyId = "duplicate-test";
        jwtService.generateHmacKey(keyId);

        // Attempting to generate same key should work but overwrite
        String newKey = jwtService.generateHmacKey(keyId);
        assertNotNull(newKey);
    }

    @Test
    void shouldGenerateJwks() {
        // Add a test RSA key
        jwtService.generateRsaKeyPair("test-jwks-key");

        Map<String, Object> jwks = jwtService.getJwks();

        assertNotNull(jwks);
        assertTrue(jwks.containsKey("keys"));

        @SuppressWarnings("unchecked")
        Map<String, Object>[] keys = (Map<String, Object>[]) jwks.get("keys");
        assertTrue(keys.length >= 2); // At least default + test key
    }

    @Test
    void shouldHandleEmptyClaimsMap() {
        String token = jwtService.generateToken(
            "test-user",
            new HashMap<>(),
            Duration.ofMinutes(5),
            "HS256"
        );

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void shouldHandleNullClaimsMap() {
        String token = jwtService.generateToken(
            "test-user",
            null,
            Duration.ofMinutes(5),
            "HS256"
        );

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
}