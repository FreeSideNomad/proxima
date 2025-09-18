package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    private ConfigurationService configurationService;

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
        return ResponseEntity.ok(headers != null ? headers : Map.of());
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getConfigInfo() {
        Map<String, Object> info = Map.of(
                "downstreamUrl", configurationService.getDownstreamUrl(),
                "activePreset", configurationService.getActivePresetName(),
                "totalPresets", configurationService.getAllPresets() != null ?
                               configurationService.getAllPresets().size() : 0
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
}