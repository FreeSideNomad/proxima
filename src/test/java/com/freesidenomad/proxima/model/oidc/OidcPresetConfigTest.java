package com.freesidenomad.proxima.model.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OidcPresetConfigTest {

    private OidcPresetConfig.OidcPresetConfigBuilder builder;

    @BeforeEach
    void setUp() {
        builder = OidcPresetConfig.builder()
                .enabled(true)
                .subject("test-user-123")
                .tokenExpirationSeconds(3600L)
                .algorithm("RS256")
                .email("test@example.com")
                .name("Test User")
                .preferredUsername("testuser");
    }

    @Test
    void shouldCreateOidcPresetConfigWithDefaults() {
        OidcPresetConfig config = OidcPresetConfig.builder().build();

        assertFalse(config.isEnabled());
        assertEquals(3600L, config.getTokenExpirationSeconds());
        assertEquals("RS256", config.getAlgorithm());
        assertEquals("default", config.getKeyId());
        assertEquals(Arrays.asList("openid", "profile", "email"), config.getScopes());
        assertTrue(config.getGroups().isEmpty());
        assertTrue(config.getCustomClaims().isEmpty());
    }

    @Test
    void shouldCreateOidcPresetConfigWithCustomValues() {
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("department", "engineering");
        customClaims.put("role", "developer");

        OidcPresetConfig config = builder
                .groups(Arrays.asList("users", "developers"))
                .customClaims(customClaims)
                .clientId("test-client")
                .redirectUri("http://localhost:8080/callback")
                .build();

        assertTrue(config.isEnabled());
        assertEquals("test-user-123", config.getSubject());
        assertEquals("test@example.com", config.getEmail());
        assertEquals("Test User", config.getName());
        assertEquals("testuser", config.getPreferredUsername());
        assertEquals(Arrays.asList("users", "developers"), config.getGroups());
        assertEquals("engineering", config.getCustomClaims().get("department"));
        assertEquals("developer", config.getCustomClaims().get("role"));
        assertEquals("test-client", config.getClientId());
        assertEquals("http://localhost:8080/callback", config.getRedirectUri());
    }

    @Test
    void shouldConvertToOidcClaims() {
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("department", "engineering");
        customClaims.put("level", "senior");

        OidcPresetConfig config = builder
                .groups(Arrays.asList("users", "developers"))
                .customClaims(customClaims)
                .build();

        String issuer = "http://localhost:8080";
        List<String> audience = Arrays.asList("test-client");

        Map<String, Object> claims = config.toClaims(issuer, audience);

        assertEquals("test-user-123", claims.get("sub"));
        assertEquals(issuer, claims.get("iss"));
        assertEquals(audience, claims.get("aud"));
        assertNotNull(claims.get("exp"));
        assertNotNull(claims.get("iat"));

        Map<String, Object> allClaims = claims;
        assertEquals("test@example.com", allClaims.get("email"));
        assertEquals("Test User", allClaims.get("name"));
        assertEquals("testuser", allClaims.get("preferred_username"));
        assertEquals(Arrays.asList("users", "developers"), allClaims.get("groups"));
        assertEquals("engineering", allClaims.get("department"));
        assertEquals("senior", allClaims.get("level"));
    }

    @Test
    void shouldHandleNullStandardClaimsInConversion() {
        OidcPresetConfig config = OidcPresetConfig.builder()
                .enabled(true)
                .subject("test-user")
                .tokenExpirationSeconds(1800L)
                .build();

        String issuer = "http://localhost:8080";
        List<String> audience = Arrays.asList("test-client");

        Map<String, Object> claims = config.toClaims(issuer, audience);

        assertEquals("test-user", claims.get("sub"));
        assertEquals(issuer, claims.get("iss"));

        Map<String, Object> allClaims = claims;
        assertFalse(allClaims.containsKey("email"));
        assertFalse(allClaims.containsKey("name"));
        assertFalse(allClaims.containsKey("preferred_username"));
        assertFalse(allClaims.containsKey("groups"));
    }

    @Test
    void shouldSetCorrectExpirationTime() {
        OidcPresetConfig config = builder
                .tokenExpirationSeconds(7200L)
                .build();

        String issuer = "http://localhost:8080";
        List<String> audience = Arrays.asList("test-client");

        Instant beforeConversion = Instant.now();
        Map<String, Object> claims = config.toClaims(issuer, audience);
        Instant afterConversion = Instant.now();

        long expectedMinExp = beforeConversion.plusSeconds(7200L).getEpochSecond();
        long expectedMaxExp = afterConversion.plusSeconds(7200L).getEpochSecond();
        long actualExp = (Long) claims.get("exp");

        assertTrue(actualExp >= expectedMinExp);
        assertTrue(actualExp <= expectedMaxExp);
    }

    @Test
    void shouldSetCorrectIssuedAtTime() {
        OidcPresetConfig config = builder.build();

        String issuer = "http://localhost:8080";
        List<String> audience = Arrays.asList("test-client");

        Instant beforeConversion = Instant.now();
        Map<String, Object> claims = config.toClaims(issuer, audience);
        Instant afterConversion = Instant.now();

        long actualIat = (Long) claims.get("iat");
        assertTrue(actualIat >= beforeConversion.getEpochSecond());
        assertTrue(actualIat <= afterConversion.getEpochSecond());
    }

    @Test
    void shouldHandleEmptyGroupsInConversion() {
        OidcPresetConfig config = builder
                .groups(new ArrayList<>())
                .build();

        String issuer = "http://localhost:8080";
        List<String> audience = Arrays.asList("test-client");

        Map<String, Object> claims = config.toClaims(issuer, audience);

        Map<String, Object> allClaims = claims;
        assertFalse(allClaims.containsKey("groups"));
    }

    @Test
    void shouldMergeCustomClaimsCorrectly() {
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("custom_field", "custom_value");
        customClaims.put("email", "custom@example.com"); // Should be overridden

        OidcPresetConfig config = builder
                .customClaims(customClaims)
                .build();

        String issuer = "http://localhost:8080";
        List<String> audience = Arrays.asList("test-client");

        Map<String, Object> claims = config.toClaims(issuer, audience);

        Map<String, Object> allClaims = claims;
        assertEquals("test@example.com", allClaims.get("email")); // Standard claim takes precedence
        assertEquals("custom_value", allClaims.get("custom_field"));
    }

    @Test
    void shouldHandleNullCustomClaims() {
        OidcPresetConfig config = builder
                .customClaims(null)
                .build();

        assertNotNull(config.getCustomClaims());
        assertTrue(config.getCustomClaims().isEmpty());
    }
}