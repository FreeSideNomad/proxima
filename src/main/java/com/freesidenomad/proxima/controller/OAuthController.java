package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.oidc.AuthorizationCode;
import com.freesidenomad.proxima.model.oidc.OidcTokens;
import com.freesidenomad.proxima.service.AuthorizationCodeService;
import com.freesidenomad.proxima.service.ConfigurationService;
import com.freesidenomad.proxima.service.OidcTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@Tag(name = "OAuth", description = "OAuth 2.0 / OIDC authorization endpoints")
public class OAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @Autowired
    private AuthorizationCodeService authorizationCodeService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OidcTokenService oidcTokenService;

    @GetMapping("/authorize")
    @Operation(
        summary = "OAuth Authorization Endpoint",
        description = "Initiates the OAuth 2.0 authorization code flow"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirect to callback URL with authorization code"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> authorize(
            @Parameter(description = "Response type (must be 'code')", required = true)
            @RequestParam("response_type") String responseType,

            @Parameter(description = "Client identifier", required = true)
            @RequestParam("client_id") String clientId,

            @Parameter(description = "Redirect URI", required = true)
            @RequestParam("redirect_uri") String redirectUri,

            @Parameter(description = "Requested scopes")
            @RequestParam(value = "scope", required = false, defaultValue = "openid") String scope,

            @Parameter(description = "State parameter for CSRF protection")
            @RequestParam(value = "state", required = false) String state,

            @Parameter(description = "Nonce for replay protection")
            @RequestParam(value = "nonce", required = false) String nonce) {

        try {
            // Validate response_type
            if (!"code".equals(responseType)) {
                return createErrorResponse(redirectUri, "unsupported_response_type",
                    "Only 'code' response type is supported", state);
            }

            // Find preset with matching client_id
            HeaderPreset preset = findPresetByClientId(clientId);
            if (preset == null || !preset.isOidcEnabled()) {
                return createErrorResponse(redirectUri, "invalid_client",
                    "Invalid client_id or OIDC not enabled", state);
            }

            // Validate redirect_uri
            String configuredRedirectUri = preset.getOidcConfig().getRedirectUri();
            if (!redirectUri.equals(configuredRedirectUri)) {
                return createErrorResponse(redirectUri, "invalid_redirect_uri",
                    "Redirect URI mismatch", state);
            }

            // Generate authorization code
            String subject = preset.getOidcConfig().getSubject();
            AuthorizationCode authCode = authorizationCodeService.generateAuthorizationCode(
                clientId, redirectUri, scope, state, nonce, subject);

            // Build redirect URL with authorization code
            String redirectUrl = buildRedirectUrl(redirectUri, authCode.getCode(), state);

            logger.info("Authorization granted for client: {} redirecting to: {}", clientId, redirectUri);

            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();

        } catch (Exception e) {
            logger.error("Error in authorization endpoint", e);
            return createErrorResponse(redirectUri, "server_error",
                "Internal server error", state);
        }
    }

    @PostMapping("/token")
    @Operation(
        summary = "OAuth Token Endpoint",
        description = "Exchanges authorization code for access and ID tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token response"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Invalid client credentials")
    })
    public ResponseEntity<Map<String, Object>> token(
            @Parameter(description = "Grant type (must be 'authorization_code')", required = true)
            @RequestParam("grant_type") String grantType,

            @Parameter(description = "Authorization code", required = true)
            @RequestParam("code") String code,

            @Parameter(description = "Client identifier", required = true)
            @RequestParam("client_id") String clientId,

            @Parameter(description = "Redirect URI", required = true)
            @RequestParam("redirect_uri") String redirectUri) {

        try {
            // Validate grant_type
            if (!"authorization_code".equals(grantType)) {
                return createTokenErrorResponse("unsupported_grant_type",
                    "Only 'authorization_code' grant type is supported");
            }

            // Validate and consume authorization code
            AuthorizationCode authCode = authorizationCodeService.validateAndConsumeCode(
                code, clientId, redirectUri);

            // Find preset for token generation
            HeaderPreset preset = findPresetByClientId(clientId);
            if (preset == null || !preset.isOidcEnabled()) {
                return createTokenErrorResponse("invalid_client", "Invalid client");
            }

            // Generate tokens
            OidcTokens tokens = oidcTokenService.generateTokensForPreset(preset);

            // Build token response
            Map<String, Object> response = new HashMap<>();
            response.put("access_token", tokens.getAccessToken());
            response.put("id_token", tokens.getIdToken());
            response.put("token_type", tokens.getTokenType());
            response.put("expires_in", tokens.getExpiresIn());
            response.put("scope", authCode.getScope());

            if (tokens.getRefreshToken() != null) {
                response.put("refresh_token", tokens.getRefreshToken());
            }

            logger.info("Tokens issued for client: {} with scope: {}", clientId, authCode.getScope());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Token request validation failed", e);
            return createTokenErrorResponse("invalid_grant", e.getMessage());
        } catch (Exception e) {
            logger.error("Error in token endpoint", e);
            return createTokenErrorResponse("server_error", "Internal server error");
        }
    }

    private HeaderPreset findPresetByClientId(String clientId) {
        return configurationService.getAllPresets().stream()
            .filter(preset -> preset.isOidcEnabled() &&
                             clientId.equals(preset.getOidcConfig().getClientId()))
            .findFirst()
            .orElse(null);
    }

    private String buildRedirectUrl(String redirectUri, String code, String state) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));

        if (state != null) {
            url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }

        return url.toString();
    }

    private ResponseEntity<?> createErrorResponse(String redirectUri, String error,
                                                String errorDescription, String state) {
        try {
            StringBuilder url = new StringBuilder(redirectUri);
            url.append(redirectUri.contains("?") ? "&" : "?");
            url.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
            url.append("&error_description=").append(URLEncoder.encode(errorDescription, StandardCharsets.UTF_8));

            if (state != null) {
                url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url.toString())
                .build();
        } catch (Exception e) {
            // Fallback to JSON error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", error);
            errorResponse.put("error_description", errorDescription);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private ResponseEntity<Map<String, Object>> createTokenErrorResponse(String error, String errorDescription) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("error_description", errorDescription);
        errorResponse.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(errorResponse);
    }
}