package com.freesidenomad.proxima.config;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.RouteRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "proxima")
public class ProximaProperties {

    private Downstream downstream = new Downstream();
    private Map<String, String> headers;
    private List<HeaderPreset> presets;
    private String activePreset;
    private List<RouteRule> routes;

    public Downstream getDownstream() {
        return downstream;
    }

    public void setDownstream(Downstream downstream) {
        this.downstream = downstream;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public List<HeaderPreset> getPresets() {
        return presets;
    }

    public void setPresets(List<HeaderPreset> presets) {
        this.presets = presets;
    }

    public String getActivePreset() {
        return activePreset;
    }

    public void setActivePreset(String activePreset) {
        this.activePreset = activePreset;
    }

    public List<RouteRule> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteRule> routes) {
        this.routes = routes;
    }

    public static class Downstream {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}