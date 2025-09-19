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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RouteService routeService;

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

        long startTime = System.currentTimeMillis();
        String clientIp = getClientIpAddress(originalRequest);

        String targetUrl = routeService.resolveTargetUrl(path);
        if (targetUrl == null) {
            logger.info("BLOCKED: {} {} from {} - Reserved route", method, path, clientIp);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(404).body("{\"error\":\"Route not found\"}")
            );
        }

        HttpHeaders headers = buildHeaders(originalRequest);

        logger.info("PROXY: {} {} from {} -> {} (headers: {})",
                   method, path, clientIp, targetUrl,
                   configurationService.getActivePresetName());

        if (body != null && !body.isEmpty()) {
            logger.debug("Request body length: {} bytes", body.length());
        }

        return webClient
                .method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase(java.util.Locale.ENGLISH)))
                .uri(targetUrl)
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .body(body != null ? BodyInserters.fromValue(body) : BodyInserters.empty())
                .exchangeToMono(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("PROXY SUCCESS: {} {} from {} -> {} completed in {}ms (status: {})",
                               method, path, clientIp, targetUrl, duration, response.statusCode());

                    // Return response as-is to pass through HTTP errors transparently
                    return response.toEntity(String.class);
                })
                .timeout(Duration.ofSeconds(30))
                .toFuture()
                .exceptionally(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.error("PROXY ERROR: {} {} from {} -> {} failed after {}ms: {}",
                               method, path, clientIp, targetUrl, duration, throwable.getMessage());
                    return ResponseEntity.status(500).body("Proxy error: " + throwable.getMessage());
                });
    }

    private HttpHeaders buildHeaders(HttpServletRequest originalRequest) {
        HttpHeaders headers = new HttpHeaders();
        Map<String, String> headerMappings = configurationService.getActiveHeaderMappings();
        Map<String, String> currentHeaders = configurationService.getCurrentHeaders();

        Collections.list(originalRequest.getHeaderNames()).forEach(headerName -> {
            String headerValue = originalRequest.getHeader(headerName);
            if (!isHopByHopHeader(headerName)) {
                // Check if this header should be remapped
                String mappedHeaderName = headerMappings.getOrDefault(headerName, headerName);

                // Only add the mapped header if it's not going to be overridden by preset headers
                if (currentHeaders == null || !currentHeaders.containsKey(mappedHeaderName)) {
                    headers.add(mappedHeaderName, headerValue);

                    if (!headerName.equals(mappedHeaderName)) {
                        logger.debug("Header remapped: {} -> {}", headerName, mappedHeaderName);
                    }
                } else if (!headerName.equals(mappedHeaderName)) {
                    // Still add the mapped header even if preset will override the original name
                    headers.add(mappedHeaderName, headerValue);
                    logger.debug("Header remapped: {} -> {}", headerName, mappedHeaderName);
                }
            }
        });

        // Add preset headers (these override any incoming headers with same name)
        if (currentHeaders != null) {
            currentHeaders.forEach((key, value) -> {
                logger.debug("Adding custom header: {} = {}", key, value);
                headers.set(key, value); // Use set to override any existing headers
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

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}