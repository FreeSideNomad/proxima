package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.oidc.OidcDiscoveryResponse;
import com.freesidenomad.proxima.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;

@RestController
@Tag(name = "OIDC Discovery", description = "OpenID Connect Discovery endpoints")
public class OidcDiscoveryController {

    @Autowired
    private JwtService jwtService;

    @Value("${proxima.oidc.issuer:#{null}}")
    private String configuredIssuer;

    @GetMapping(value = "/.well-known/openid_configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "OpenID Connect Discovery",
        description = "Returns OpenID Connect provider metadata"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Provider metadata returned successfully")
    })
    public ResponseEntity<OidcDiscoveryResponse> openidConfiguration(HttpServletRequest request) {
        String issuer = getIssuerUrl(request);

        OidcDiscoveryResponse response = OidcDiscoveryResponse.builder()
            .issuer(issuer)
            .authorizationEndpoint(issuer + "/oauth/authorize")
            .tokenEndpoint(issuer + "/oauth/token")
            .jwksUri(issuer + "/oauth/jwks")
            .responseTypesSupported(Arrays.asList("code"))
            .subjectTypesSupported(Arrays.asList("public"))
            .idTokenSigningAlgValuesSupported(Arrays.asList("RS256"))
            .scopesSupported(Arrays.asList("openid", "profile", "email"))
            .grantTypesSupported(Arrays.asList("authorization_code"))
            .responseModesSupported(Arrays.asList("query"))
            .tokenEndpointAuthMethodsSupported(Arrays.asList("client_secret_post", "none"))
            .claimsSupported(Arrays.asList(
                "sub", "iss", "aud", "exp", "iat", "auth_time", "nonce",
                "email", "name", "preferred_username", "groups"
            ))
            .codeChallengeMethodsSupported(Arrays.asList("S256", "plain"))
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/oauth/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "JSON Web Key Set",
        description = "Returns the public keys used for token verification"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "JWKS returned successfully")
    })
    public ResponseEntity<Map<String, Object>> jwks() {
        Map<String, Object> jwks = jwtService.getJwks();
        return ResponseEntity.ok(jwks);
    }

    private String getIssuerUrl(HttpServletRequest request) {
        if (configuredIssuer != null && !configuredIssuer.isEmpty()) {
            return configuredIssuer;
        }

        // Build issuer URL from request
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        StringBuilder issuer = new StringBuilder();
        issuer.append(scheme).append("://").append(serverName);

        if ((scheme.equals("http") && serverPort != 80) ||
            (scheme.equals("https") && serverPort != 443)) {
            issuer.append(":").append(serverPort);
        }

        return issuer.toString();
    }
}