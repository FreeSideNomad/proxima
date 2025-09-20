package com.freesidenomad.proxima.filter;

import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.service.ProxyService;
import com.freesidenomad.proxima.service.RouteService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Filter that intercepts HTTP requests and proxies them to configured downstream services.
 * Only handles requests that match configured routes. All other requests are passed through
 * to Spring controllers, allowing Swagger UI and other Spring endpoints to work normally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnBean({RouteService.class, ProxyService.class})
public class ProxyFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyFilter.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private ProxyService proxyService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            path += "?" + queryString;
        }

        // Check if this path matches any configured route
        Optional<ProximaConfig.ConfigRoute> matchedRoute = routeService.findMatchingRouteWithPriority(path);

        if (matchedRoute.isPresent()) {
            // This is a configured proxy route, handle it
            ProximaConfig.ConfigRoute route = matchedRoute.get();
            logger.debug("Proxy filter handling request: {} -> {}", path, route.getTargetUrl());

            try {
                // Read request body if present
                String body = null;
                if (request.getContentLength() > 0) {
                    body = StreamUtils.copyToString(request.getInputStream(), request.getCharacterEncoding() != null ?
                            java.nio.charset.Charset.forName(request.getCharacterEncoding()) :
                            java.nio.charset.StandardCharsets.UTF_8);
                }

                // Use the proxy service to forward the request
                CompletableFuture<ResponseEntity<String>> futureResponse =
                        proxyService.forwardRequest(request.getMethod(), path, request, body);

                // Wait for the response (blocking call in filter)
                ResponseEntity<String> proxyResponse = futureResponse.get();

                // Copy response status
                response.setStatus(proxyResponse.getStatusCodeValue());

                // Copy response headers, but skip transfer-encoding to avoid conflicts with content-length
                proxyResponse.getHeaders().forEach((name, values) -> {
                    // Skip transfer-encoding header since we're using content-length from Spring's toEntity conversion
                    if (!"transfer-encoding".equalsIgnoreCase(name)) {
                        for (String value : values) {
                            response.addHeader(name, value);
                        }
                    }
                });

                // Copy response body
                String responseBody = proxyResponse.getBody();
                if (responseBody != null) {
                    response.getWriter().write(responseBody);
                }

                // Request has been handled, don't continue the filter chain
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Request interrupted while proxying to {}: {}", route.getTargetUrl(), e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Request interrupted");
                return;
            } catch (java.util.concurrent.ExecutionException e) {
                logger.error("Error proxying request to {}: {}", route.getTargetUrl(), e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Proxy Error: " + e.getMessage());
                return;
            } catch (IOException e) {
                logger.error("IO error while proxying request to {}: {}", route.getTargetUrl(), e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "IO Error: " + e.getMessage());
                return;
            }
        }

        // Not a proxy route, continue with normal Spring processing
        logger.debug("Path {} not configured for proxying, passing to Spring controllers", path);
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("ProxyFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("ProxyFilter destroyed");
    }
}