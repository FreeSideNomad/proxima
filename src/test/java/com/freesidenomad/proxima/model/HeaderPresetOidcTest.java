package com.freesidenomad.proxima.model;

import com.freesidenomad.proxima.model.oidc.OidcPresetConfig;
import com.freesidenomad.proxima.model.oidc.OidcTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderPresetOidcTest {

    private HeaderPreset headerPreset;
    private OidcPresetConfig oidcConfig;

    @BeforeEach
    void setUp() {
        headerPreset = new HeaderPreset();
        headerPreset.setName("test-preset");
        headerPreset.setDisplayName("Test Preset");

        Map<String, String> headers = new HashMap<>();
        headers.put("X-User-ID", "test-user");
        headerPreset.setHeaders(headers);

        oidcConfig = OidcPresetConfig.builder()
                .enabled(true)
                .subject("test-user-123")
                .tokenExpirationSeconds(3600L)
                .algorithm("RS256")
                .email("test@example.com")
                .name("Test User")
                .preferredUsername("testuser")
                .groups(Arrays.asList("users", "developers"))
                .build();
    }

    @Test
    void shouldExtendHeaderPresetWithOidcSupport() {
        headerPreset.setOidcConfig(oidcConfig);

        assertNotNull(headerPreset.getOidcConfig());
        assertTrue(headerPreset.isOidcEnabled());
        assertEquals("test-user-123", headerPreset.getOidcConfig().getSubject());
        assertEquals(3600L, headerPreset.getOidcConfig().getTokenExpirationSeconds());
    }

    @Test
    void shouldMaintainBackwardCompatibility() {
        assertNull(headerPreset.getOidcConfig());
        assertFalse(headerPreset.isOidcEnabled());
        assertNull(headerPreset.getValidAccessToken());

        assertNotNull(headerPreset.getHeaders());
        assertEquals("test-user", headerPreset.getHeaders().get("X-User-ID"));
    }

    @Test
    void shouldCacheTokensWithExpiration() {
        headerPreset.setOidcConfig(oidcConfig);

        OidcTokens tokens = OidcTokens.builder()
                .accessToken("valid-access-token")
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        headerPreset.cacheTokens(tokens);

        String cachedToken = headerPreset.getValidAccessToken();
        assertEquals("valid-access-token", cachedToken);
    }

    @Test
    void shouldReturnNullForExpiredTokens() {
        headerPreset.setOidcConfig(oidcConfig);

        OidcTokens expiredTokens = OidcTokens.builder()
                .accessToken("expired-access-token")
                .tokenType("Bearer")
                .expiresAt(Instant.now().minusSeconds(100))
                .build();

        headerPreset.cacheTokens(expiredTokens);

        String cachedToken = headerPreset.getValidAccessToken();
        assertNull(cachedToken);
    }

    @Test
    void shouldNotCacheTokensWhenOidcDisabled() {
        OidcPresetConfig disabledConfig = OidcPresetConfig.builder()
                .enabled(false)
                .subject("test-user")
                .build();

        headerPreset.setOidcConfig(disabledConfig);

        OidcTokens tokens = OidcTokens.builder()
                .accessToken("access-token")
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        headerPreset.cacheTokens(tokens);

        assertNull(headerPreset.getValidAccessToken());
    }

    @Test
    void shouldHandleNullOidcConfig() {
        headerPreset.setOidcConfig(null);

        assertFalse(headerPreset.isOidcEnabled());
        assertNull(headerPreset.getValidAccessToken());

        OidcTokens tokens = OidcTokens.builder()
                .accessToken("access-token")
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertDoesNotThrow(() -> headerPreset.cacheTokens(tokens));
    }

    @Test
    void shouldUpdateCachedTokens() {
        headerPreset.setOidcConfig(oidcConfig);

        OidcTokens firstTokens = OidcTokens.builder()
                .accessToken("first-token")
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        headerPreset.cacheTokens(firstTokens);
        assertEquals("first-token", headerPreset.getValidAccessToken());

        OidcTokens secondTokens = OidcTokens.builder()
                .accessToken("second-token")
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        headerPreset.cacheTokens(secondTokens);
        assertEquals("second-token", headerPreset.getValidAccessToken());
    }

    @Test
    void shouldHandleNullTokenCache() {
        headerPreset.setOidcConfig(oidcConfig);

        String token = headerPreset.getValidAccessToken();
        assertNull(token);
    }
}