package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.RouteRule;
import com.freesidenomad.proxima.service.ConfigurationService;
import com.freesidenomad.proxima.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proxima/api/config")
@Tag(name = "Configuration", description = "Proxy configuration and preset management")
public class ConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RouteService routeService;

    @GetMapping("/presets")
    public ResponseEntity<List<HeaderPreset>> getAllPresets() {
        List<HeaderPreset> presets = configurationService.getAllPresets();
        return ResponseEntity.ok(presets);
    }

    @GetMapping("/presets/{name}")
    public ResponseEntity<HeaderPreset> getPreset(@PathVariable String name) {
        return configurationService.getPresetByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active-preset")
    public ResponseEntity<Map<String, Object>> getActivePreset() {
        HeaderPreset activePreset = configurationService.getActivePreset();
        String activePresetName = configurationService.getActivePresetName();

        if (activePreset != null) {
            Map<String, Object> response = Map.of(
                    "name", activePresetName,
                    "preset", activePreset
            );
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(Map.of("name", "none", "preset", Map.of()));
    }

    @PostMapping("/presets/{name}/activate")
    public ResponseEntity<Map<String, String>> activatePreset(@PathVariable String name) {
        logger.info("Request to activate preset: {}", name);

        boolean success = configurationService.setActivePreset(name);

        if (success) {
            Map<String, String> response = Map.of(
                    "status", "success",
                    "message", "Preset '" + name + "' activated successfully",
                    "activePreset", name
            );
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = Map.of(
                    "status", "error",
                    "message", "Preset '" + name + "' not found"
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/headers")
    public ResponseEntity<Map<String, String>> getCurrentHeaders() {
        Map<String, String> headers = configurationService.getCurrentHeaders();
        return ResponseEntity.ok(headers);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getConfigInfo() {
        Map<String, Object> info = Map.of(
                "downstreamUrl", configurationService.getDownstreamUrl(),
                "activePreset", configurationService.getActivePresetName(),
                "totalPresets", configurationService.getAllPresets() != null ?
                               configurationService.getAllPresets().size() : 0,
                "totalRoutes", routeService.getRouteCount(),
                "enabledRoutes", routeService.getEnabledRouteCount()
        );
        return ResponseEntity.ok(info);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateConfiguration() {
        List<String> errors = configurationService.validateConfiguration();

        Map<String, Object> response = Map.of(
                "valid", errors.isEmpty(),
                "errors", errors
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteRule>> getAllRoutes() {
        List<RouteRule> routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes != null ? routes : List.of());
    }

    @GetMapping("/routes/test/{path}")
    public ResponseEntity<Map<String, Object>> testRoute(@PathVariable String path) {
        String fullPath = "/" + path;
        String resolvedUrl = routeService.resolveTargetUrl(fullPath);

        Map<String, Object> result = Map.of(
                "inputPath", fullPath,
                "resolvedUrl", resolvedUrl,
                "matchingRoute", routeService.findMatchingRoute(fullPath)
                        .map(route -> Map.of(
                                "pattern", route.getPathPattern(),
                                "target", route.getTargetUrl(),
                                "description", route.getDescription()
                        ))
                        .orElse(Map.of("message", "No specific route found, using default"))
        );

        return ResponseEntity.ok(result);
    }
}