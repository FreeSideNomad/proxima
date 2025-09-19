package com.freesidenomad.proxima.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "headers")
public class HeaderPreset {
    private String name;
    private String displayName;
    @NonNull
    private Map<String, String> headers = new HashMap<>();
    private boolean active;

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
}