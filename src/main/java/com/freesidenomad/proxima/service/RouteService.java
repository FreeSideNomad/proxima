package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.RouteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    @Autowired
    private ProximaProperties proximaProperties;

    public String resolveTargetUrl(String path) {
        if (proximaProperties.getRoutes() != null) {
            for (RouteRule route : proximaProperties.getRoutes()) {
                if (route.matches(path)) {
                    String targetUrl = route.buildTargetUrl(path);
                    logger.debug("Route matched: {} -> {} (using rule: {})",
                               path, targetUrl, route.getPathPattern());
                    return targetUrl;
                }
            }
        }

        // Fallback to default downstream URL
        String fallbackUrl = proximaProperties.getDownstream().getUrl() + path;
        logger.debug("No route matched for {}, using default: {}", path, fallbackUrl);
        return fallbackUrl;
    }

    public List<RouteRule> getAllRoutes() {
        return proximaProperties.getRoutes();
    }

    public Optional<RouteRule> findMatchingRoute(String path) {
        if (proximaProperties.getRoutes() != null) {
            return proximaProperties.getRoutes().stream()
                    .filter(route -> route.matches(path))
                    .findFirst();
        }
        return Optional.empty();
    }

    public boolean hasRoutes() {
        return proximaProperties.getRoutes() != null && !proximaProperties.getRoutes().isEmpty();
    }

    public int getRouteCount() {
        return proximaProperties.getRoutes() != null ? proximaProperties.getRoutes().size() : 0;
    }

    public long getEnabledRouteCount() {
        if (proximaProperties.getRoutes() == null) {
            return 0;
        }
        return proximaProperties.getRoutes().stream().filter(RouteRule::isEnabled).count();
    }
}