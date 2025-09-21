package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.oidc.AuthorizationCode;
import com.freesidenomad.proxima.model.oidc.OidcPresetConfig;
import com.freesidenomad.proxima.model.oidc.OidcTokens;
import com.freesidenomad.proxima.service.AuthorizationCodeService;
import com.freesidenomad.proxima.service.ConfigurationService;
import com.freesidenomad.proxima.service.OidcTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class OAuthControllerTest {

    @MockBean
    private AuthorizationCodeService authorizationCodeService;

    @MockBean
    private ConfigurationService configurationService;

    @MockBean
    private OidcTokenService oidcTokenService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuthController oAuthController;

    private HeaderPreset oidcPreset;
    private OidcPresetConfig oidcConfig;

    @BeforeEach
    void setUp() {
        oidcConfig = OidcPresetConfig.builder()
            .enabled(true)
            .subject("test@example.com")
            .clientId("test-client")
            .redirectUri("http://localhost:8080/callback")
            .scopes(Arrays.asList("openid", "profile", "email"))
            .build();

        oidcPreset = new HeaderPreset();
        oidcPreset.setName("test-preset");
        oidcPreset.setOidcConfig(oidcConfig);
    }

    @Test
    void shouldRedirectToCallbackWithAuthorizationCode() throws Exception {
        // Arrange
        List<HeaderPreset> presets = Arrays.asList(oidcPreset);
        when(configurationService.getAllPresets()).thenReturn(presets);

        AuthorizationCode authCode = AuthorizationCode.create(
            "test-client", "http://localhost:8080/callback", "openid profile", "state123", "nonce456", "test@example.com");
        when(authorizationCodeService.generateAuthorizationCode(
            eq("test-client"), eq("http://localhost:8080/callback"), eq("openid profile"),
            eq("state123"), eq("nonce456"), eq("test@example.com")))
            .thenReturn(authCode);

        // Act & Assert
        mockMvc.perform(get("/oauth/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback")
                .param("scope", "openid profile")
                .param("state", "state123")
                .param("nonce", "nonce456"))
            .andExpect(status().isFound())
            .andExpect(result -> {
                String redirectUrl = result.getResponse().getHeader("Location");
                assertNotNull(redirectUrl, "Redirect URL should not be null");
                assertTrue(redirectUrl.startsWith("http://localhost:8080/callback?code="));
                assertTrue(redirectUrl.contains("state=state123"));
            });

        verify(authorizationCodeService).generateAuthorizationCode(
            eq("test-client"), eq("http://localhost:8080/callback"), eq("openid profile"),
            eq("state123"), eq("nonce456"), eq("test@example.com"));
    }

    @Test
    void shouldReturnErrorForUnsupportedResponseType() throws Exception {
        when(configurationService.getAllPresets()).thenReturn(Arrays.asList(oidcPreset));

        mockMvc.perform(get("/oauth/authorize")
                .param("response_type", "token")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isFound())
            .andExpect(result -> {
                String redirectUrl = result.getResponse().getHeader("Location");
                assertNotNull(redirectUrl, "Redirect URL should not be null");
                assertTrue(redirectUrl.contains("error=unsupported_response_type"));
            });
    }

    @Test
    void shouldReturnErrorForInvalidClient() throws Exception {
        when(configurationService.getAllPresets()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/oauth/authorize")
                .param("response_type", "code")
                .param("client_id", "invalid-client")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isFound())
            .andExpect(result -> {
                String redirectUrl = result.getResponse().getHeader("Location");
                assertNotNull(redirectUrl, "Redirect URL should not be null");
                assertTrue(redirectUrl.contains("error=invalid_client"));
            });
    }

    @Test
    void shouldReturnErrorForRedirectUriMismatch() throws Exception {
        List<HeaderPreset> presets = Arrays.asList(oidcPreset);
        when(configurationService.getAllPresets()).thenReturn(presets);

        mockMvc.perform(get("/oauth/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://evil.com/callback"))
            .andExpect(status().isFound())
            .andExpect(result -> {
                String redirectUrl = result.getResponse().getHeader("Location");
                assertNotNull(redirectUrl, "Redirect URL should not be null");
                assertTrue(redirectUrl.contains("error=invalid_redirect_uri"));
            });
    }

    @Test
    void shouldExchangeCodeForTokens() {
        // Arrange
        String authCode = "test-auth-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";

        AuthorizationCode validCode = AuthorizationCode.builder()
            .code(authCode)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scope("openid profile")
            .subject("test@example.com")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .used(false)
            .build();

        when(authorizationCodeService.validateAndConsumeCode(authCode, clientId, redirectUri))
            .thenReturn(validCode);

        List<HeaderPreset> presets = Arrays.asList(oidcPreset);
        when(configurationService.getAllPresets()).thenReturn(presets);

        OidcTokens tokens = OidcTokens.builder()
            .accessToken("access-token-123")
            .idToken("id-token-456")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();

        when(oidcTokenService.generateTokensForPreset(oidcPreset)).thenReturn(tokens);

        // Act
        ResponseEntity<Map<String, Object>> response = oAuthController.token(
            "authorization_code", authCode, clientId, redirectUri);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("access-token-123", body.get("access_token"));
        assertEquals("id-token-456", body.get("id_token"));
        assertEquals("Bearer", body.get("token_type"));
        assertEquals(3600L, body.get("expires_in"));
        assertEquals("openid profile", body.get("scope"));

        verify(authorizationCodeService).validateAndConsumeCode(authCode, clientId, redirectUri);
        verify(oidcTokenService).generateTokensForPreset(oidcPreset);
    }

    @Test
    void shouldReturnErrorForUnsupportedGrantType() {
        ResponseEntity<Map<String, Object>> response = oAuthController.token(
            "client_credentials", "code", "client", "redirect");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("unsupported_grant_type", body.get("error"));
    }

    @Test
    void shouldReturnErrorForInvalidAuthorizationCode() {
        when(authorizationCodeService.validateAndConsumeCode(anyString(), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid authorization code"));

        ResponseEntity<Map<String, Object>> response = oAuthController.token(
            "authorization_code", "invalid-code", "client", "redirect");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("invalid_grant", body.get("error"));
        assertEquals("Invalid authorization code", body.get("error_description"));
    }

    @Test
    void shouldReturnErrorWhenPresetNotFound() {
        String authCode = "test-auth-code";
        String clientId = "test-client";
        String redirectUri = "http://localhost:8080/callback";

        AuthorizationCode validCode = AuthorizationCode.builder()
            .code(authCode)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scope("openid profile")
            .subject("test@example.com")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .used(false)
            .build();

        when(authorizationCodeService.validateAndConsumeCode(authCode, clientId, redirectUri))
            .thenReturn(validCode);
        when(configurationService.getAllPresets()).thenReturn(Arrays.asList());

        ResponseEntity<Map<String, Object>> response = oAuthController.token(
            "authorization_code", authCode, clientId, redirectUri);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("invalid_client", body.get("error"));
    }
}