package com.freesidenomad.proxima.controller;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtControllerModelTest {

    // Test TokenRequest class methods for coverage
    @Test
    void testTokenRequest_Creation() {
        JwtController.TokenRequest request = new JwtController.TokenRequest();

        assertNotNull(request.getClaims());
        assertTrue(request.getClaims().isEmpty());
    }

    @Test
    void testTokenRequest_SettersAndGetters() {
        JwtController.TokenRequest request = new JwtController.TokenRequest();

        request.setSubject("test-subject");
        request.setAlgorithm("HS256");
        request.setKeyId("test-key");
        request.setExpirationSeconds(7200L);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "user");
        request.setClaims(claims);

        assertEquals("test-subject", request.getSubject());
        assertEquals("HS256", request.getAlgorithm());
        assertEquals("test-key", request.getKeyId());
        assertEquals(7200L, request.getExpirationSeconds());
        assertEquals("user", request.getClaims().get("role"));
    }

    @Test
    void testTokenRequest_EqualsAndHashCode() {
        JwtController.TokenRequest request1 = new JwtController.TokenRequest();
        request1.setSubject("test");
        request1.setAlgorithm("HS256");

        JwtController.TokenRequest request2 = new JwtController.TokenRequest();
        request2.setSubject("test");
        request2.setAlgorithm("HS256");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testTokenRequest_NotEquals() {
        JwtController.TokenRequest request1 = new JwtController.TokenRequest();
        request1.setSubject("test1");

        JwtController.TokenRequest request2 = new JwtController.TokenRequest();
        request2.setSubject("test2");

        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testTokenRequest_ToString() {
        JwtController.TokenRequest request = new JwtController.TokenRequest();
        request.setSubject("test");
        request.setAlgorithm("HS256");

        String toString = request.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("test"));
        assertTrue(toString.contains("HS256"));
    }

    @Test
    void testTokenRequest_CanEqual() {
        JwtController.TokenRequest request1 = new JwtController.TokenRequest();
        JwtController.TokenRequest request2 = new JwtController.TokenRequest();

        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual("not a TokenRequest"));
    }

    @Test
    void testTokenRequest_SetClaimsDefensiveCopy() {
        JwtController.TokenRequest request = new JwtController.TokenRequest();

        Map<String, Object> originalClaims = new HashMap<>();
        originalClaims.put("role", "admin");
        request.setClaims(originalClaims);

        // Modify original map
        originalClaims.put("role", "user");

        // Request should still have the original value due to defensive copying
        assertEquals("admin", request.getClaims().get("role"));
    }

    @Test
    void testTokenRequest_SetClaimsNull() {
        JwtController.TokenRequest request = new JwtController.TokenRequest();
        request.setClaims(null);

        assertNotNull(request.getClaims());
        assertTrue(request.getClaims().isEmpty());
    }

    // Test KeyRequest class as well
    @Test
    void testKeyRequest_Creation() {
        JwtController.KeyRequest keyRequest = new JwtController.KeyRequest();
        assertNotNull(keyRequest);
    }

    @Test
    void testKeyRequest_SettersAndGetters() {
        JwtController.KeyRequest keyRequest = new JwtController.KeyRequest();
        keyRequest.setKeyId("test-key-id");

        assertEquals("test-key-id", keyRequest.getKeyId());
    }

    @Test
    void testKeyRequest_DefaultBehavior() {
        JwtController.KeyRequest request1 = new JwtController.KeyRequest();
        request1.setKeyId("test-key");

        JwtController.KeyRequest request2 = new JwtController.KeyRequest();
        request2.setKeyId("test-key");

        // KeyRequest doesn't have Lombok annotations, so just test basic functionality
        assertEquals("test-key", request1.getKeyId());
        assertEquals("test-key", request2.getKeyId());
    }

    @Test
    void testKeyRequest_ToString() {
        JwtController.KeyRequest keyRequest = new JwtController.KeyRequest();
        keyRequest.setKeyId("test-key");

        String toString = keyRequest.toString();
        assertNotNull(toString);
        // Default toString from Object class won't contain the keyId
        assertTrue(toString.contains("KeyRequest"));
    }
}