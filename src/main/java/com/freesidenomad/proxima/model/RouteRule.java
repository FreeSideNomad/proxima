package com.freesidenomad.proxima.model;

public class RouteRule {
    private String pathPattern;
    private String targetUrl;
    private String description;
    private boolean enabled = true;

    public RouteRule() {}

    public RouteRule(String pathPattern, String targetUrl, String description) {
        this.pathPattern = pathPattern;
        this.targetUrl = targetUrl;
        this.description = description;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean matches(String path) {
        if (!enabled || pathPattern == null) {
            return false;
        }

        // Support wildcard patterns
        if (pathPattern.endsWith("/**")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 3);
            return path.startsWith(prefix);
        }

        // Exact match
        return path.equals(pathPattern);
    }

    public String buildTargetUrl(String originalPath) {
        if (pathPattern.endsWith("/**")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 3);
            String remainder = originalPath.substring(prefix.length());
            return targetUrl + remainder;
        }

        return targetUrl;
    }
}