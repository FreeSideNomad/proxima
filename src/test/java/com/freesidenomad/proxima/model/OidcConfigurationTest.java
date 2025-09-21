package com.freesidenomad.proxima.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Arrays;

class OidcConfigurationTest {

    private OidcConfiguration oidcConfiguration;

    @BeforeEach
    void setUp() {
        oidcConfiguration = new OidcConfiguration();
    }

    @Test
    void shouldCreateDefaultOidcConfiguration() {
        // Test default values
        assertNotNull(oidcConfiguration);
        assertEquals("http://localhost:8080", oidcConfiguration.getIssuer());
        assertEquals("proxima-client", oidcConfiguration.getClientId());
        assertEquals(3600, oidcConfiguration.getDefaultExpiry());
        assertTrue(oidcConfiguration.getSupportedScopes().contains("openid"));
        assertTrue(oidcConfiguration.getSupportedGrantTypes().contains("authorization_code"));
        assertTrue(oidcConfiguration.getSupportedResponseTypes().contains("code"));
    }

    @Test
    void shouldSetAndGetIssuer() {
        String issuer = "https://example.com";
        oidcConfiguration.setIssuer(issuer);
        assertEquals(issuer, oidcConfiguration.getIssuer());
    }

    @Test
    void shouldSetAndGetClientId() {
        String clientId = "test-client";
        oidcConfiguration.setClientId(clientId);
        assertEquals(clientId, oidcConfiguration.getClientId());
    }

    @Test
    void shouldSetAndGetDefaultExpiry() {
        int expiry = 7200;
        oidcConfiguration.setDefaultExpiry(expiry);
        assertEquals(expiry, oidcConfiguration.getDefaultExpiry());
    }

    @Test
    void shouldSetAndGetSupportedScopes() {
        List<String> scopes = Arrays.asList("openid", "profile", "email", "offline_access");
        oidcConfiguration.setSupportedScopes(scopes);
        assertEquals(scopes, oidcConfiguration.getSupportedScopes());
    }

    @Test
    void shouldSetAndGetSupportedGrantTypes() {
        List<String> grantTypes = Arrays.asList("authorization_code", "refresh_token");
        oidcConfiguration.setSupportedGrantTypes(grantTypes);
        assertEquals(grantTypes, oidcConfiguration.getSupportedGrantTypes());
    }

    @Test
    void shouldSetAndGetSupportedResponseTypes() {
        List<String> responseTypes = Arrays.asList("code", "id_token");
        oidcConfiguration.setSupportedResponseTypes(responseTypes);
        assertEquals(responseTypes, oidcConfiguration.getSupportedResponseTypes());
    }

    @Test
    void shouldValidateMinimumExpiry() {
        // Should not allow expiry less than 300 seconds (5 minutes)
        assertThrows(IllegalArgumentException.class, () -> {
            oidcConfiguration.setDefaultExpiry(299);
        });
    }

    @Test
    void shouldValidateMaximumExpiry() {
        // Should not allow expiry more than 86400 seconds (24 hours)
        assertThrows(IllegalArgumentException.class, () -> {
            oidcConfiguration.setDefaultExpiry(86401);
        });
    }

    @Test
    void shouldValidateIssuerUrl() {
        // Should throw exception for invalid URL
        assertThrows(IllegalArgumentException.class, () -> {
            oidcConfiguration.setIssuer("not-a-url");
        });
    }

    @Test
    void shouldRequireOpenidScope() {
        // Should throw exception if 'openid' scope is not included
        List<String> scopesWithoutOpenid = Arrays.asList("profile", "email");
        assertThrows(IllegalArgumentException.class, () -> {
            oidcConfiguration.setSupportedScopes(scopesWithoutOpenid);
        });
    }

    @Test
    void shouldRequireAuthorizationCodeGrantType() {
        // Should throw exception if 'authorization_code' grant type is not included
        List<String> grantTypesWithoutAuthCode = Arrays.asList("refresh_token");
        assertThrows(IllegalArgumentException.class, () -> {
            oidcConfiguration.setSupportedGrantTypes(grantTypesWithoutAuthCode);
        });
    }

    @Test
    void shouldRequireCodeResponseType() {
        // Should throw exception if 'code' response type is not included
        List<String> responseTypesWithoutCode = Arrays.asList("id_token", "token");
        assertThrows(IllegalArgumentException.class, () -> {
            oidcConfiguration.setSupportedResponseTypes(responseTypesWithoutCode);
        });
    }

    @Test
    void shouldGenerateAuthorizationEndpoint() {
        oidcConfiguration.setIssuer("https://example.com");
        assertEquals("https://example.com/oauth2/authorize", oidcConfiguration.getAuthorizationEndpoint());
    }

    @Test
    void shouldGenerateTokenEndpoint() {
        oidcConfiguration.setIssuer("https://example.com");
        assertEquals("https://example.com/oauth2/token", oidcConfiguration.getTokenEndpoint());
    }

    @Test
    void shouldGenerateJwksUri() {
        oidcConfiguration.setIssuer("https://example.com");
        assertEquals("https://example.com/.well-known/jwks.json", oidcConfiguration.getJwksUri());
    }

    @Test
    void shouldGenerateDiscoveryEndpoint() {
        oidcConfiguration.setIssuer("https://example.com");
        assertEquals("https://example.com/.well-known/openid-configuration", oidcConfiguration.getDiscoveryEndpoint());
    }

    @Test
    void shouldCreateBuilderPattern() {
        OidcConfiguration config = OidcConfiguration.builder()
                .issuer("https://test.com")
                .clientId("test-client")
                .defaultExpiry(1800)
                .supportedScopes(Arrays.asList("openid", "profile"))
                .supportedGrantTypes(Arrays.asList("authorization_code"))
                .supportedResponseTypes(Arrays.asList("code"))
                .build();

        assertEquals("https://test.com", config.getIssuer());
        assertEquals("test-client", config.getClientId());
        assertEquals(1800, config.getDefaultExpiry());
    }

    @Test
    void shouldEqualsSameConfiguration() {
        OidcConfiguration config1 = new OidcConfiguration();
        config1.setIssuer("https://example.com");
        config1.setClientId("test-client");

        OidcConfiguration config2 = new OidcConfiguration();
        config2.setIssuer("https://example.com");
        config2.setClientId("test-client");

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void shouldNotEqualsDifferentConfiguration() {
        OidcConfiguration config1 = new OidcConfiguration();
        config1.setIssuer("https://example.com");

        OidcConfiguration config2 = new OidcConfiguration();
        config2.setIssuer("https://different.com");

        assertNotEquals(config1, config2);
    }

    @Test
    void shouldProvideToString() {
        oidcConfiguration.setIssuer("https://example.com");
        oidcConfiguration.setClientId("test-client");

        String toString = oidcConfiguration.toString();
        assertTrue(toString.contains("https://example.com"));
        assertTrue(toString.contains("test-client"));
    }
}