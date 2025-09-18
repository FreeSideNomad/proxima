package com.freesidenomad.proxima.model;

import java.util.Map;

public class HeaderPreset {
    private String name;
    private String displayName;
    private Map<String, String> headers;
    private boolean active;

    public HeaderPreset() {}

    public HeaderPreset(String name, String displayName, Map<String, String> headers) {
        this.name = name;
        this.displayName = displayName;
        this.headers = headers;
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}