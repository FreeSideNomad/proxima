package com.freesidenomad.proxima.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.freesidenomad.proxima.model.oidc.OidcPresetConfig;
import com.freesidenomad.proxima.model.oidc.OidcTokens;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"headers", "cachedTokens"})
public class HeaderPreset {
    private String name;
    private String displayName;
    @NonNull
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> headerMappings = new HashMap<>();
    private boolean active;

    private OidcPresetConfig oidcConfig;

    @JsonIgnore
    private Map<String, OidcTokens> cachedTokens = new ConcurrentHashMap<>();

    public HeaderPreset(String name, String displayName, Map<String, String> headers) {
        this.name = name;
        this.displayName = displayName;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.active = false;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    public boolean isOidcEnabled() {
        return oidcConfig != null && oidcConfig.isEnabled();
    }

    public String getValidAccessToken() {
        if (!isOidcEnabled()) {
            return null;
        }

        OidcTokens tokens = cachedTokens.get("current");
        if (tokens == null || tokens.isExpired()) {
            return null;
        }

        return tokens.getAccessToken();
    }

    public void cacheTokens(OidcTokens tokens) {
        if (isOidcEnabled() && tokens != null) {
            cachedTokens.put("current", tokens);
        }
    }
}