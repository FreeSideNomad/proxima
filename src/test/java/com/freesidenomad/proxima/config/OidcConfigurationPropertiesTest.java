package com.freesidenomad.proxima.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

@SpringBootTest
@TestPropertySource(properties = {
    "proxima.oidc.issuer=https://test.example.com",
    "proxima.oidc.client-id=test-client-123",
    "proxima.oidc.default-expiry=7200",
    "proxima.oidc.supported-scopes=openid,profile,email,custom",
    "proxima.oidc.supported-grant-types=authorization_code,refresh_token",
    "proxima.oidc.supported-response-types=code,id_token"
})
class OidcConfigurationPropertiesTest {

    @Autowired
    private OidcConfigurationProperties oidcConfigurationProperties;

    @Test
    void shouldLoadConfigurationFromProperties() {
        assertNotNull(oidcConfigurationProperties);
        assertEquals("https://test.example.com", oidcConfigurationProperties.getIssuer());
        assertEquals("test-client-123", oidcConfigurationProperties.getClientId());
        assertEquals(7200, oidcConfigurationProperties.getDefaultExpiry());
    }

    @Test
    void shouldLoadSupportedScopes() {
        assertEquals(Arrays.asList("openid", "profile", "email", "custom"),
                    oidcConfigurationProperties.getSupportedScopes());
    }

    @Test
    void shouldLoadSupportedGrantTypes() {
        assertEquals(Arrays.asList("authorization_code", "refresh_token"),
                    oidcConfigurationProperties.getSupportedGrantTypes());
    }

    @Test
    void shouldLoadSupportedResponseTypes() {
        assertEquals(Arrays.asList("code", "id_token"),
                    oidcConfigurationProperties.getSupportedResponseTypes());
    }

    @Test
    void shouldGenerateEndpoints() {
        assertEquals("https://test.example.com/oauth2/authorize",
                    oidcConfigurationProperties.getAuthorizationEndpoint());
        assertEquals("https://test.example.com/oauth2/token",
                    oidcConfigurationProperties.getTokenEndpoint());
        assertEquals("https://test.example.com/.well-known/jwks.json",
                    oidcConfigurationProperties.getJwksUri());
        assertEquals("https://test.example.com/.well-known/openid-configuration",
                    oidcConfigurationProperties.getDiscoveryEndpoint());
    }

    @Test
    void shouldConvertToOidcConfiguration() {
        var oidcConfig = oidcConfigurationProperties.toOidcConfiguration();

        assertNotNull(oidcConfig);
        assertEquals(oidcConfigurationProperties.getIssuer(), oidcConfig.getIssuer());
        assertEquals(oidcConfigurationProperties.getClientId(), oidcConfig.getClientId());
        assertEquals(oidcConfigurationProperties.getDefaultExpiry(), oidcConfig.getDefaultExpiry());
        assertEquals(oidcConfigurationProperties.getSupportedScopes(), oidcConfig.getSupportedScopes());
        assertEquals(oidcConfigurationProperties.getSupportedGrantTypes(), oidcConfig.getSupportedGrantTypes());
        assertEquals(oidcConfigurationProperties.getSupportedResponseTypes(), oidcConfig.getSupportedResponseTypes());
    }
}

@SpringBootTest
class OidcConfigurationPropertiesDefaultsTest {

    @Autowired
    private OidcConfigurationProperties oidcConfigurationProperties;

    @Test
    void shouldUseDefaultValues() {
        assertNotNull(oidcConfigurationProperties);
        assertEquals("http://localhost:8080", oidcConfigurationProperties.getIssuer());
        assertEquals("proxima-client", oidcConfigurationProperties.getClientId());
        assertEquals(3600, oidcConfigurationProperties.getDefaultExpiry());
        assertTrue(oidcConfigurationProperties.getSupportedScopes().contains("openid"));
        assertTrue(oidcConfigurationProperties.getSupportedGrantTypes().contains("authorization_code"));
        assertTrue(oidcConfigurationProperties.getSupportedResponseTypes().contains("code"));
    }
}