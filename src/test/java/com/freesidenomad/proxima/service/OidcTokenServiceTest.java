package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.oidc.OidcPresetConfig;
import com.freesidenomad.proxima.model.oidc.OidcTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcTokenServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private OidcTokenServiceImpl oidcTokenService;

    private HeaderPreset oidcEnabledPreset;
    private HeaderPreset regularPreset;
    private OidcPresetConfig oidcConfig;

    @BeforeEach
    void setUp() {
        // Create OIDC-enabled preset
        oidcConfig = OidcPresetConfig.builder()
                .enabled(true)
                .subject("test-user-123")
                .tokenExpirationSeconds(3600L)
                .algorithm("RS256")
                .keyId("default")
                .email("test@example.com")
                .name("Test User")
                .preferredUsername("testuser")
                .clientId("test-client")
                .scopes(Arrays.asList("openid", "profile", "email"))
                .build();

        oidcEnabledPreset = new HeaderPreset();
        oidcEnabledPreset.setName("oidc-preset");
        oidcEnabledPreset.setDisplayName("OIDC Enabled Preset");
        oidcEnabledPreset.setOidcConfig(oidcConfig);

        // Create regular preset (no OIDC)
        regularPreset = new HeaderPreset();
        regularPreset.setName("regular-preset");
        regularPreset.setDisplayName("Regular Preset");
        regularPreset.setHeaders(Map.of("X-Test", "value"));
    }

    @Test
    void shouldGenerateIdAndAccessTokensForPreset() {
        // Given
        String expectedIdToken = "id.token.jwt";
        String expectedAccessToken = "access.token.jwt";

        when(jwtService.generateToken(eq("test-user-123"), any(Map.class),
                eq(Duration.ofSeconds(3600)), eq("RS256"), eq("default")))
                .thenReturn(expectedIdToken)
                .thenReturn(expectedAccessToken);

        // When
        OidcTokens result = oidcTokenService.generateTokensForPreset(oidcEnabledPreset);

        // Then
        assertNotNull(result);
        assertEquals(expectedAccessToken, result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(3600L, result.getExpiresIn());
        assertEquals("openid profile email", result.getScope());
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(Instant.now()));

        // Verify JWT service was called twice (ID token and access token)
        verify(jwtService, times(2)).generateToken(
                eq("test-user-123"),
                any(Map.class),
                eq(Duration.ofSeconds(3600)),
                eq("RS256"),
                eq("default")
        );
    }

    @Test
    void shouldThrowExceptionForNonOidcEnabledPreset() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> oidcTokenService.generateTokensForPreset(regularPreset)
        );

        assertEquals("Preset does not have OIDC enabled: regular-preset", exception.getMessage());
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldCacheTokensWithProperExpiration() {
        // Given
        String accessToken = "cached.access.token";
        when(jwtService.generateToken(any(), any(), any(), any(), any()))
                .thenReturn(accessToken);

        // When - Generate tokens first time
        OidcTokens firstCall = oidcTokenService.generateTokensForPreset(oidcEnabledPreset);

        // Then - Verify tokens are cached in preset
        assertNotNull(oidcEnabledPreset.getValidAccessToken());
        assertEquals(accessToken, oidcEnabledPreset.getValidAccessToken());

        // When - Get cached tokens (should use cache, not call service again)
        OidcTokens cachedTokens = oidcTokenService.getValidTokensForPreset("oidc-preset");

        // Then - Should return cached tokens without calling JWT service again
        assertNotNull(cachedTokens);
        assertEquals(accessToken, cachedTokens.getAccessToken());
        verify(jwtService, times(2)).generateToken(any(), any(), any(), any(), any()); // Only original calls
    }

    @Test
    void shouldRefreshTokensBeforeExpiration() {
        // Given - Create preset with tokens expiring soon
        OidcTokens expiredTokens = OidcTokens.builder()
                .accessToken("expired.token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .expiresAt(Instant.now().minusSeconds(10)) // Already expired
                .scope("openid profile email")
                .build();

        oidcEnabledPreset.cacheTokens(expiredTokens);

        when(configurationService.getPreset("oidc-preset")).thenReturn(oidcEnabledPreset);
        when(jwtService.generateToken(any(), any(), any(), any(), any()))
                .thenReturn("new.access.token");

        // When
        OidcTokens result = oidcTokenService.getValidTokensForPreset("oidc-preset");

        // Then - Should generate new tokens
        assertNotNull(result);
        assertEquals("new.access.token", result.getAccessToken());
        verify(jwtService, times(2)).generateToken(any(), any(), any(), any(), any());
    }

    @Test
    void shouldGenerateTokensOnStartup() {
        // Given
        List<HeaderPreset> oidcPresets = Arrays.asList(oidcEnabledPreset);
        when(configurationService.getOidcEnabledPresets()).thenReturn(oidcPresets);
        when(jwtService.generateToken(any(), any(), any(), any(), any()))
                .thenReturn("startup.token");

        // When
        oidcTokenService.generateTokensForAllPresets();

        // Then
        verify(configurationService).getOidcEnabledPresets();
        verify(jwtService, times(2)).generateToken(any(), any(), any(), any(), any());
        assertNotNull(oidcEnabledPreset.getValidAccessToken());
    }

    @Test
    void shouldIntegrateWithExistingJwtService() {
        // Given
        when(jwtService.generateToken(
                eq("test-user-123"),
                argThat(claims ->
                    claims.containsKey("iss") &&
                    claims.containsKey("aud") &&
                    claims.containsKey("exp") &&
                    claims.containsKey("iat") &&
                    claims.containsKey("sub") &&
                    "test@example.com".equals(claims.get("email")) &&
                    "Test User".equals(claims.get("name"))
                ),
                eq(Duration.ofSeconds(3600)),
                eq("RS256"),
                eq("default")
        )).thenReturn("jwt.token");

        // When
        OidcTokens result = oidcTokenService.generateTokensForPreset(oidcEnabledPreset);

        // Then
        assertNotNull(result);
        assertEquals("jwt.token", result.getAccessToken());

        // Verify correct claims were passed to JWT service
        verify(jwtService, times(2)).generateToken(
                eq("test-user-123"),
                argThat(claims -> claims.containsKey("iss") && claims.containsKey("aud")),
                eq(Duration.ofSeconds(3600)),
                eq("RS256"),
                eq("default")
        );
    }

    @Test
    void shouldHandleTokenExpirationGracefully() {
        // Given - Preset with no tokens initially
        when(configurationService.getPreset("oidc-preset")).thenReturn(oidcEnabledPreset);
        when(jwtService.generateToken(any(), any(), any(), any(), any()))
                .thenReturn("fresh.token");

        // When - Get tokens for preset with no cached tokens
        OidcTokens result = oidcTokenService.getValidTokensForPreset("oidc-preset");

        // Then - Should generate new tokens
        assertNotNull(result);
        assertEquals("fresh.token", result.getAccessToken());
        verify(jwtService, times(2)).generateToken(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnNullForNonExistentPreset() {
        // Given
        when(configurationService.getPreset("non-existent")).thenReturn(null);

        // When
        OidcTokens result = oidcTokenService.getValidTokensForPreset("non-existent");

        // Then
        assertNull(result);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldReturnNullForNonOidcPreset() {
        // Given
        when(configurationService.getPreset("regular-preset")).thenReturn(regularPreset);

        // When
        OidcTokens result = oidcTokenService.getValidTokensForPreset("regular-preset");

        // Then
        assertNull(result);
        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldClearTokensForPreset() {
        // Given
        when(jwtService.generateToken(any(), any(), any(), any(), any())).thenReturn("token");
        oidcTokenService.generateTokensForPreset(oidcEnabledPreset);

        // When
        oidcTokenService.clearTokensForPreset("oidc-preset");

        // Then - Should regenerate tokens on next access
        when(configurationService.getPreset("oidc-preset")).thenReturn(oidcEnabledPreset);
        oidcEnabledPreset.cacheTokens(null); // Clear preset cache too

        OidcTokens result = oidcTokenService.getValidTokensForPreset("oidc-preset");

        // Should call JWT service again (2 original + 2 new)
        verify(jwtService, times(4)).generateToken(any(), any(), any(), any(), any());
    }

    @Test
    void shouldProvideTokenCacheStatistics() {
        // Given - Generate tokens for preset
        when(jwtService.generateToken(any(), any(), any(), any(), any())).thenReturn("token");
        oidcTokenService.generateTokensForPreset(oidcEnabledPreset);

        // When
        Map<String, Object> stats = oidcTokenService.getTokenCacheStats();

        // Then
        assertNotNull(stats);
        assertEquals(1, stats.get("cachedPresets"));
        assertTrue(((List<?>) stats.get("presets")).contains("oidc-preset"));
        assertEquals(1L, stats.get("validTokens"));
        assertEquals(0L, stats.get("expiredTokens"));
    }

    @Test
    void shouldHandleStartupGenerationErrors() {
        // Given
        HeaderPreset faultyPreset = new HeaderPreset();
        faultyPreset.setName("faulty-preset");
        faultyPreset.setOidcConfig(OidcPresetConfig.builder().enabled(true).build()); // Missing required fields

        List<HeaderPreset> presets = Arrays.asList(oidcEnabledPreset, faultyPreset);
        when(configurationService.getOidcEnabledPresets()).thenReturn(presets);
        when(jwtService.generateToken(any(), any(), any(), any(), any()))
                .thenReturn("good.token")
                .thenThrow(new RuntimeException("Token generation failed"));

        // When - Should not throw exception even if one preset fails
        assertDoesNotThrow(() -> oidcTokenService.generateTokensForAllPresets());

        // Then - Should have attempted to generate for both presets
        verify(configurationService).getOidcEnabledPresets();
    }
}