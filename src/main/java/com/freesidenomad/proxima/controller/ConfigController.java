package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.service.JsonConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/proxima/api/config")
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    @Autowired
    private JsonConfigurationService jsonConfigurationService;

    @GetMapping
    public ResponseEntity<ProximaConfig> getConfiguration() {
        try {
            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("Error loading configuration: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> updateConfiguration(@RequestBody ProximaConfig config) {
        try {
            // Validate that routes don't use reserved patterns
            for (ProximaConfig.ConfigRoute route : config.getRoutes()) {
                if (!jsonConfigurationService.isValidRoute(route.getPathPattern())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Route pattern conflicts with reserved routes: " + route.getPathPattern()));
                }
            }

            jsonConfigurationService.saveConfiguration(config);
            logger.info("Configuration updated successfully");
            return ResponseEntity.ok(Map.of("message", "Configuration updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating configuration: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update configuration: " + e.getMessage()));
        }
    }

    @PostMapping("/routes")
    public ResponseEntity<?> addRoute(@RequestBody ProximaConfig.ConfigRoute route) {
        try {
            if (!jsonConfigurationService.isValidRoute(route.getPathPattern())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Route pattern conflicts with reserved routes: " + route.getPathPattern()));
            }

            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            config.getRoutes().add(route);
            jsonConfigurationService.saveConfiguration(config);

            logger.info("Route added successfully: {}", route.getPathPattern());
            return ResponseEntity.ok(Map.of("message", "Route added successfully"));
        } catch (Exception e) {
            logger.error("Error adding route: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to add route: " + e.getMessage()));
        }
    }

    @DeleteMapping("/routes/{index}")
    public ResponseEntity<?> deleteRoute(@PathVariable int index) {
        try {
            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            if (index < 0 || index >= config.getRoutes().size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid route index"));
            }

            ProximaConfig.ConfigRoute removedRoute = config.getRoutes().remove(index);
            jsonConfigurationService.saveConfiguration(config);

            logger.info("Route deleted successfully: {}", removedRoute.getPathPattern());
            return ResponseEntity.ok(Map.of("message", "Route deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting route: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to delete route: " + e.getMessage()));
        }
    }

    @PostMapping("/presets")
    public ResponseEntity<?> addPreset(@RequestBody ProximaConfig.ConfigHeaderPreset preset) {
        try {
            ProximaConfig config = jsonConfigurationService.loadConfiguration();

            // Check if preset name already exists
            boolean exists = config.getPresets().stream()
                    .anyMatch(p -> p.getName().equals(preset.getName()));
            if (exists) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Preset name already exists: " + preset.getName()));
            }

            config.getPresets().add(preset);
            jsonConfigurationService.saveConfiguration(config);

            logger.info("Preset added successfully: {}", preset.getName());
            return ResponseEntity.ok(Map.of("message", "Preset added successfully"));
        } catch (Exception e) {
            logger.error("Error adding preset: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to add preset: " + e.getMessage()));
        }
    }

    @DeleteMapping("/presets/{index}")
    public ResponseEntity<?> deletePreset(@PathVariable int index) {
        try {
            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            if (index < 0 || index >= config.getPresets().size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid preset index"));
            }

            ProximaConfig.ConfigHeaderPreset removedPreset = config.getPresets().remove(index);

            // If this was the active preset, switch to the first available one
            if (config.getActivePreset().equals(removedPreset.getName())) {
                if (!config.getPresets().isEmpty()) {
                    config.setActivePreset(config.getPresets().get(0).getName());
                } else {
                    config.setActivePreset(null);
                }
            }

            jsonConfigurationService.saveConfiguration(config);

            logger.info("Preset deleted successfully: {}", removedPreset.getName());
            return ResponseEntity.ok(Map.of("message", "Preset deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting preset: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to delete preset: " + e.getMessage()));
        }
    }

    @PostMapping("/active-preset")
    public ResponseEntity<?> setActivePreset(@RequestBody Map<String, String> request) {
        try {
            String presetName = request.get("presetName");
            if (presetName == null || presetName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Preset name is required"));
            }

            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            boolean exists = config.getPresets().stream()
                    .anyMatch(p -> p.getName().equals(presetName));

            if (!exists) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Preset not found: " + presetName));
            }

            config.setActivePreset(presetName);
            jsonConfigurationService.saveConfiguration(config);

            logger.info("Active preset changed to: {}", presetName);
            return ResponseEntity.ok(Map.of("message", "Active preset updated successfully"));
        } catch (Exception e) {
            logger.error("Error setting active preset: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to set active preset: " + e.getMessage()));
        }
    }

    @GetMapping("/reserved-routes")
    public ResponseEntity<Map<String, Object>> getReservedRoutes() {
        try {
            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            return ResponseEntity.ok(Map.of("reservedRoutes", config.getReservedRoutes()));
        } catch (Exception e) {
            logger.error("Error getting reserved routes: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}