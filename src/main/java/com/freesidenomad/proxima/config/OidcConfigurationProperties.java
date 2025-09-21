package com.freesidenomad.proxima.config;

import com.freesidenomad.proxima.model.OidcConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Component
@ConfigurationProperties(prefix = "proxima.oidc")
@Data
public class OidcConfigurationProperties {

    private String issuer = "http://localhost:8080";
    private String clientId = "proxima-client";
    private int defaultExpiry = 3600;
    private List<String> supportedScopes = new ArrayList<>(Arrays.asList("openid", "profile", "email"));
    private List<String> supportedGrantTypes = new ArrayList<>(Arrays.asList("authorization_code"));
    private List<String> supportedResponseTypes = new ArrayList<>(Arrays.asList("code"));

    public String getAuthorizationEndpoint() {
        return normalizeIssuer(issuer) + "/oauth2/authorize";
    }

    public String getTokenEndpoint() {
        return normalizeIssuer(issuer) + "/oauth2/token";
    }

    public String getJwksUri() {
        return normalizeIssuer(issuer) + "/.well-known/jwks.json";
    }

    public String getDiscoveryEndpoint() {
        return normalizeIssuer(issuer) + "/.well-known/openid-configuration";
    }

    public OidcConfiguration toOidcConfiguration() {
        return OidcConfiguration.builder()
                .issuer(issuer)
                .clientId(clientId)
                .defaultExpiry(defaultExpiry)
                .supportedScopes(supportedScopes)
                .supportedGrantTypes(supportedGrantTypes)
                .supportedResponseTypes(supportedResponseTypes)
                .build();
    }

    private String normalizeIssuer(String issuer) {
        return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }
}