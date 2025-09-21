package com.freesidenomad.proxima.model.oidc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OidcDiscoveryResponse {

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;

    @JsonProperty("subject_types_supported")
    private List<String> subjectTypesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgValuesSupported;

    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;

    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;

    @JsonProperty("response_modes_supported")
    private List<String> responseModesSupported;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("claims_supported")
    private List<String> claimsSupported;

    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;
}