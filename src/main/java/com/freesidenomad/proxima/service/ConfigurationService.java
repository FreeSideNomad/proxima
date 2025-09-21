package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.validation.ConfigurationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private ProximaProperties proximaProperties;

    @Autowired
    private JsonConfigurationService jsonConfigurationService;

    @Autowired
    private ConfigurationValidator validator;

    public List<HeaderPreset> getAllPresets() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getPresets().stream()
                .map(this::convertToHeaderPreset)
                .collect(Collectors.toList());
    }

    public Optional<HeaderPreset> getPresetByName(String name) {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getPresets().stream()
                .filter(preset -> preset.getName().equals(name))
                .map(this::convertToHeaderPreset)
                .findFirst();
    }

    public HeaderPreset getActivePreset() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        if (config.getActivePreset() != null) {
            Optional<HeaderPreset> preset = getPresetByName(config.getActivePreset());
            if (preset.isPresent()) {
                return preset.get();
            }
        }

        if (!config.getPresets().isEmpty()) {
            ProximaConfig.ConfigHeaderPreset firstPreset = config.getPresets().get(0);
            logger.info("No active preset configured, using first preset: {}", firstPreset.getName());
            return convertToHeaderPreset(firstPreset);
        }

        return null;
    }

    public Map<String, String> getCurrentHeaders() {
        HeaderPreset activePreset = getActivePreset();
        if (activePreset != null) {
            logger.debug("Using headers from active preset: {}", activePreset.getName());
            return activePreset.getHeaders();
        }

        logger.debug("Using fallback headers from legacy configuration");
        return proximaProperties.getHeaders();
    }

    public boolean setActivePreset(String presetName) {
        try {
            ProximaConfig config = jsonConfigurationService.loadConfiguration();
            Optional<ProximaConfig.ConfigHeaderPreset> preset = config.getPresets().stream()
                    .filter(p -> p.getName().equals(presetName))
                    .findFirst();

            if (preset.isPresent()) {
                config.setActivePreset(presetName);
                jsonConfigurationService.saveConfiguration(config);
                logger.info("Active preset changed to: {}", presetName);
                return true;
            }

            logger.warn("Attempted to set non-existent preset as active: {}", presetName);
            return false;
        } catch (Exception e) {
            logger.error("Error setting active preset: {}", e.getMessage());
            return false;
        }
    }

    public String getActivePresetName() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getActivePreset();
    }

    public String getDownstreamUrl() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        return config.getDownstream().getUrl();
    }

    private HeaderPreset convertToHeaderPreset(ProximaConfig.ConfigHeaderPreset configPreset) {
        HeaderPreset preset = new HeaderPreset();
        preset.setName(configPreset.getName());
        preset.setDisplayName(configPreset.getDisplayName());
        preset.setHeaders(configPreset.getHeaders());
        preset.setHeaderMappings(configPreset.getHeaderMappings());
        preset.setOidcConfig(configPreset.getOidcConfig());
        return preset;
    }

    public Map<String, String> getActiveHeaderMappings() {
        ProximaConfig config = jsonConfigurationService.loadConfiguration();
        if (config.getActivePreset() != null) {
            Optional<ProximaConfig.ConfigHeaderPreset> preset = config.getPresets().stream()
                    .filter(p -> p.getName().equals(config.getActivePreset()))
                    .findFirst();
            if (preset.isPresent()) {
                return preset.get().getHeaderMappings();
            }
        }
        return new java.util.HashMap<>();
    }

    public List<String> validateConfiguration() {
        return validator.validatePresets(proximaProperties.getPresets());
    }

    /**
     * Get preset by name - alias for getPresetByName().
     * Used by OidcTokenService for consistency.
     */
    public HeaderPreset getPreset(String name) {
        return getPresetByName(name).orElse(null);
    }

    /**
     * Get all OIDC-enabled presets.
     * Used by OidcTokenService for startup token generation.
     */
    public List<HeaderPreset> getOidcEnabledPresets() {
        return getAllPresets().stream()
                .filter(HeaderPreset::isOidcEnabled)
                .collect(Collectors.toList());
    }
}