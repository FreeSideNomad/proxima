package com.freesidenomad.proxima.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ProximaConfig {

    private Downstream downstream = new Downstream();

    @JsonProperty("activePreset")
    private String activePreset = "admin_user";

    private List<ConfigHeaderPreset> presets = new ArrayList<>();
    private List<ConfigRoute> routes = new ArrayList<>();

    @JsonProperty("reservedRoutes")
    private List<String> reservedRoutes = new ArrayList<>();

    public static class Downstream {
        private String url = "http://nginx:80";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class ConfigHeaderPreset {
        private String name;

        @JsonProperty("displayName")
        private String displayName;

        private java.util.Map<String, String> headers = new java.util.HashMap<>();

        @JsonProperty("headerMappings")
        private java.util.Map<String, String> headerMappings = new java.util.HashMap<>();

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

        public java.util.Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(java.util.Map<String, String> headers) {
            this.headers = headers;
        }

        public java.util.Map<String, String> getHeaderMappings() {
            return headerMappings;
        }

        public void setHeaderMappings(java.util.Map<String, String> headerMappings) {
            this.headerMappings = headerMappings;
        }
    }

    public static class ConfigRoute {
        @JsonProperty("pathPattern")
        private String pathPattern;

        @JsonProperty("targetUrl")
        private String targetUrl;

        private String description;
        private boolean enabled = true;

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
            if (!enabled) return false;

            if (pathPattern.endsWith("/**")) {
                // Match prefix with wildcard (e.g., "/api/users/**" matches "/api/users/123")
                String prefix = pathPattern.substring(0, pathPattern.length() - 3);
                return path.startsWith(prefix);
            } else if (pathPattern.endsWith("/*")) {
                // Match prefix with single level wildcard (e.g., "/api/users/*" matches "/api/users/123" but not "/api/users/123/details")
                String prefix = pathPattern.substring(0, pathPattern.length() - 2);
                if (!path.startsWith(prefix)) return false;
                String remainder = path.substring(prefix.length());
                return !remainder.contains("/") || remainder.equals("/");
            } else if (pathPattern.contains("*")) {
                // Simple wildcard matching
                return matchesWildcard(path, pathPattern);
            } else {
                // Exact match or prefix match
                return path.equals(pathPattern) || path.startsWith(pathPattern + "/");
            }
        }

        private boolean matchesWildcard(String path, String pattern) {
            return path.matches(pattern.replace("*", ".*"));
        }

        public String buildTargetUrl(String originalPath) {
            if (pathPattern.endsWith("/**")) {
                // Forward remaining path after prefix
                String prefix = pathPattern.substring(0, pathPattern.length() - 3);
                String remainder = originalPath.substring(prefix.length());
                return targetUrl.endsWith("/") ? targetUrl + remainder.substring(1) : targetUrl + remainder;
            } else if (pathPattern.endsWith("/*")) {
                // Forward remaining path after prefix
                String prefix = pathPattern.substring(0, pathPattern.length() - 2);
                String remainder = originalPath.substring(prefix.length());
                return targetUrl.endsWith("/") ? targetUrl + remainder.substring(1) : targetUrl + remainder;
            } else if (pathPattern.contains("*")) {
                // For wildcard patterns, append the original path
                return targetUrl.endsWith("/") ? targetUrl + originalPath.substring(1) : targetUrl + originalPath;
            } else {
                // For exact or prefix matches, append remaining path
                if (originalPath.equals(pathPattern)) {
                    return targetUrl;
                } else {
                    String remainder = originalPath.substring(pathPattern.length());
                    return targetUrl.endsWith("/") ? targetUrl + remainder.substring(1) : targetUrl + remainder;
                }
            }
        }
    }

    public Downstream getDownstream() {
        return downstream;
    }

    public void setDownstream(Downstream downstream) {
        this.downstream = downstream;
    }

    public String getActivePreset() {
        return activePreset;
    }

    public void setActivePreset(String activePreset) {
        this.activePreset = activePreset;
    }

    public List<ConfigHeaderPreset> getPresets() {
        return presets;
    }

    public void setPresets(List<ConfigHeaderPreset> presets) {
        this.presets = presets;
    }

    public List<ConfigRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<ConfigRoute> routes) {
        this.routes = routes;
    }

    public List<String> getReservedRoutes() {
        return reservedRoutes;
    }

    public void setReservedRoutes(List<String> reservedRoutes) {
        this.reservedRoutes = reservedRoutes;
    }
}