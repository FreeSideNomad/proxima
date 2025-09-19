package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.model.RouteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RouteService {

    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    @Autowired
    private ProximaProperties proximaProperties;

    @Autowired
    private JsonConfigurationService jsonConfigurationService;

    public String resolveTargetUrl(String path) {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();

        // Check if path matches reserved routes
        if (isReservedRoute(path, config)) {
            logger.debug("Reserved route detected, not proxying: {}", path);
            return null;
        }

        // Check configured routes in order (first match wins)
        for (ProximaConfig.ConfigRoute route : config.getRoutes()) {
            if (route.matches(path)) {
                String targetUrl = route.buildTargetUrl(path);
                logger.info("Route matched: [{}] {} -> {} (pattern: {})",
                           route.getDescription(), path, targetUrl, route.getPathPattern());
                return targetUrl;
            }
        }

        // Fallback to default downstream URL for all other routes
        String fallbackUrl = config.getDownstream().getUrl() + path;
        logger.debug("No route matched for {}, using default: {}", path, fallbackUrl);
        return fallbackUrl;
    }

    private boolean isReservedRoute(String path, ProximaConfig config) {
        // Check against Proxima's reserved routes
        // Note: Static resources (/css/, /js/, etc.) are filtered by ProxyController, not here
        return path.startsWith("/proxima/") ||
               path.startsWith("/actuator/") ||
               path.equals("/");
    }


    public List<RouteRule> getAllRoutes() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getRoutes().stream()
                .map(this::convertToRouteRule)
                .collect(Collectors.toList());
    }

    public Optional<RouteRule> findMatchingRoute(String path) {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getRoutes().stream()
                .filter(route -> route.matches(path))
                .map(this::convertToRouteRule)
                .findFirst();
    }

    public boolean hasRoutes() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return !config.getRoutes().isEmpty();
    }

    public int getRouteCount() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getRoutes().size();
    }

    public long getEnabledRouteCount() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getRoutes().stream().filter(ProximaConfig.ConfigRoute::isEnabled).count();
    }

    private RouteRule convertToRouteRule(ProximaConfig.ConfigRoute configRoute) {
        RouteRule rule = new RouteRule();
        rule.setPathPattern(configRoute.getPathPattern());
        rule.setTargetUrl(configRoute.getTargetUrl());
        rule.setDescription(configRoute.getDescription());
        rule.setEnabled(configRoute.isEnabled());
        return rule;
    }
}