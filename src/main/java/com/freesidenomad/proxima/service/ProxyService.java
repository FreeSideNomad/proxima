package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Service
public class ProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    @Autowired
    private ProximaProperties proximaProperties;

    private final WebClient webClient;

    public ProxyService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024))
                .build();
    }

    public CompletableFuture<ResponseEntity<String>> forwardRequest(
            String method, String path, HttpServletRequest originalRequest, String body) {

        String targetUrl = proximaProperties.getDownstream().getUrl() + path;
        logger.debug("Forwarding {} request to: {}", method, targetUrl);

        HttpHeaders headers = buildHeaders(originalRequest);

        return webClient
                .method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase()))
                .uri(targetUrl)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .body(body != null ? BodyInserters.fromValue(body) : BodyInserters.empty())
                .retrieve()
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(30))
                .toFuture();
    }

    private HttpHeaders buildHeaders(HttpServletRequest originalRequest) {
        HttpHeaders headers = new HttpHeaders();

        Collections.list(originalRequest.getHeaderNames()).forEach(headerName -> {
            String headerValue = originalRequest.getHeader(headerName);
            if (!isHopByHopHeader(headerName)) {
                headers.add(headerName, headerValue);
            }
        });

        if (proximaProperties.getHeaders() != null) {
            proximaProperties.getHeaders().forEach((key, value) -> {
                logger.debug("Adding custom header: {} = {}", key, value);
                headers.set(key, value);
            });
        }

        return headers;
    }

    private boolean isHopByHopHeader(String headerName) {
        return headerName.equalsIgnoreCase("connection") ||
               headerName.equalsIgnoreCase("keep-alive") ||
               headerName.equalsIgnoreCase("proxy-authenticate") ||
               headerName.equalsIgnoreCase("proxy-authorization") ||
               headerName.equalsIgnoreCase("te") ||
               headerName.equalsIgnoreCase("trailer") ||
               headerName.equalsIgnoreCase("transfer-encoding") ||
               headerName.equalsIgnoreCase("upgrade") ||
               headerName.equalsIgnoreCase("host");
    }
}