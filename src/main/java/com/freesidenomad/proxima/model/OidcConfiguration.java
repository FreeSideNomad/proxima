package com.freesidenomad.proxima.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Data
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class OidcConfiguration {

    @NotBlank(message = "Issuer URL is required")
    private String issuer = "http://localhost:8080";

    @NotBlank(message = "Client ID is required")
    private String clientId = "proxima-client";

    @Min(value = 300, message = "Default expiry must be at least 300 seconds (5 minutes)")
    @Max(value = 86400, message = "Default expiry must not exceed 86400 seconds (24 hours)")
    private int defaultExpiry = 3600;

    @NotNull(message = "Supported scopes cannot be null")
    private List<String> supportedScopes = new ArrayList<>(Arrays.asList("openid", "profile", "email"));

    @NotNull(message = "Supported grant types cannot be null")
    private List<String> supportedGrantTypes = new ArrayList<>(Arrays.asList("authorization_code"));

    @NotNull(message = "Supported response types cannot be null")
    private List<String> supportedResponseTypes = new ArrayList<>(Arrays.asList("code"));

    public OidcConfiguration(String issuer, String clientId, int defaultExpiry,
                           List<String> supportedScopes, List<String> supportedGrantTypes,
                           List<String> supportedResponseTypes) {
        setIssuer(issuer);
        setClientId(clientId);
        setDefaultExpiry(defaultExpiry);
        setSupportedScopes(supportedScopes);
        setSupportedGrantTypes(supportedGrantTypes);
        setSupportedResponseTypes(supportedResponseTypes);
    }

    public void setIssuer(String issuer) {
        validateIssuerUrl(issuer);
        this.issuer = issuer;
    }

    public void setDefaultExpiry(int defaultExpiry) {
        if (defaultExpiry < 300) {
            throw new IllegalArgumentException("Default expiry must be at least 300 seconds (5 minutes)");
        }
        if (defaultExpiry > 86400) {
            throw new IllegalArgumentException("Default expiry must not exceed 86400 seconds (24 hours)");
        }
        this.defaultExpiry = defaultExpiry;
    }

    public void setSupportedScopes(List<String> supportedScopes) {
        if (supportedScopes == null || !supportedScopes.contains("openid")) {
            throw new IllegalArgumentException("Supported scopes must include 'openid'");
        }
        this.supportedScopes = new ArrayList<>(supportedScopes);
    }

    public void setSupportedGrantTypes(List<String> supportedGrantTypes) {
        if (supportedGrantTypes == null || !supportedGrantTypes.contains("authorization_code")) {
            throw new IllegalArgumentException("Supported grant types must include 'authorization_code'");
        }
        this.supportedGrantTypes = new ArrayList<>(supportedGrantTypes);
    }

    public void setSupportedResponseTypes(List<String> supportedResponseTypes) {
        if (supportedResponseTypes == null || !supportedResponseTypes.contains("code")) {
            throw new IllegalArgumentException("Supported response types must include 'code'");
        }
        this.supportedResponseTypes = new ArrayList<>(supportedResponseTypes);
    }

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

    private void validateIssuerUrl(String issuer) {
        if (issuer == null || issuer.trim().isEmpty()) {
            throw new IllegalArgumentException("Issuer URL cannot be null or empty");
        }

        try {
            new URL(issuer);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid issuer URL: " + issuer, e);
        }
    }

    private String normalizeIssuer(String issuer) {
        return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    public static OidcConfigurationBuilder builder() {
        return new OidcConfigurationBuilder();
    }

    public static class OidcConfigurationBuilder {
        private String issuer = "http://localhost:8080";
        private String clientId = "proxima-client";
        private int defaultExpiry = 3600;
        private List<String> supportedScopes = new ArrayList<>(Arrays.asList("openid", "profile", "email"));
        private List<String> supportedGrantTypes = new ArrayList<>(Arrays.asList("authorization_code"));
        private List<String> supportedResponseTypes = new ArrayList<>(Arrays.asList("code"));

        public OidcConfigurationBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public OidcConfigurationBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public OidcConfigurationBuilder defaultExpiry(int defaultExpiry) {
            this.defaultExpiry = defaultExpiry;
            return this;
        }

        public OidcConfigurationBuilder supportedScopes(List<String> supportedScopes) {
            this.supportedScopes = supportedScopes;
            return this;
        }

        public OidcConfigurationBuilder supportedGrantTypes(List<String> supportedGrantTypes) {
            this.supportedGrantTypes = supportedGrantTypes;
            return this;
        }

        public OidcConfigurationBuilder supportedResponseTypes(List<String> supportedResponseTypes) {
            this.supportedResponseTypes = supportedResponseTypes;
            return this;
        }

        public OidcConfiguration build() {
            return new OidcConfiguration(issuer, clientId, defaultExpiry,
                                       supportedScopes, supportedGrantTypes, supportedResponseTypes);
        }
    }
}