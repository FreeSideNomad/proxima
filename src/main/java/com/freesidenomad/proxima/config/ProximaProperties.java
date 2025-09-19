package com.freesidenomad.proxima.config;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.RouteRule;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "proxima")
@Getter
public class ProximaProperties {

    private Downstream downstream = new Downstream();
    private Map<String, String> headers = new HashMap<>();
    private List<HeaderPreset> presets = new ArrayList<>();
    private String activePreset;
    private List<RouteRule> routes = new ArrayList<>();

    public Downstream getDownstream() {
        if (downstream == null) return null;
        Downstream copy = new Downstream();
        copy.setUrl(downstream.getUrl());
        return copy;
    }

    public void setDownstream(Downstream downstream) {
        if (downstream == null) {
            this.downstream = null;
        } else {
            this.downstream = new Downstream();
            this.downstream.setUrl(downstream.getUrl());
        }
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    public List<HeaderPreset> getPresets() {
        return new ArrayList<>(presets);
    }

    public void setPresets(List<HeaderPreset> presets) {
        this.presets = presets != null ? new ArrayList<>(presets) : new ArrayList<>();
    }

    public void setActivePreset(String activePreset) {
        this.activePreset = activePreset;
    }

    public List<RouteRule> getRoutes() {
        return new ArrayList<>(routes);
    }

    public void setRoutes(List<RouteRule> routes) {
        this.routes = routes != null ? new ArrayList<>(routes) : new ArrayList<>();
    }

    @Data
    public static class Downstream {
        private String url;
    }
}