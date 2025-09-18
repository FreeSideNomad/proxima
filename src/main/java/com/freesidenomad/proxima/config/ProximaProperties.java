package com.freesidenomad.proxima.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "proxima")
public class ProximaProperties {

    private Downstream downstream = new Downstream();
    private Map<String, String> headers;

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