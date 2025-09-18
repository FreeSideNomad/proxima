package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.validation.ConfigurationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private ProximaProperties proximaProperties;

    @Autowired
    private ConfigurationValidator validator;

    public List<HeaderPreset> getAllPresets() {
        return proximaProperties.getPresets();
    }

    public Optional<HeaderPreset> getPresetByName(String name) {
        if (proximaProperties.getPresets() == null) {
            return Optional.empty();
        }

        return proximaProperties.getPresets().stream()
                .filter(preset -> preset.getName().equals(name))
                .findFirst();
    }

    public HeaderPreset getActivePreset() {
        if (proximaProperties.getActivePreset() != null) {
            Optional<HeaderPreset> preset = getPresetByName(proximaProperties.getActivePreset());
            if (preset.isPresent()) {
                return preset.get();
            }
        }

        if (proximaProperties.getPresets() != null && !proximaProperties.getPresets().isEmpty()) {
            HeaderPreset firstPreset = proximaProperties.getPresets().get(0);
            logger.info("No active preset configured, using first preset: {}", firstPreset.getName());
            return firstPreset;
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
        Optional<HeaderPreset> preset = getPresetByName(presetName);
        if (preset.isPresent()) {
            proximaProperties.setActivePreset(presetName);
            logger.info("Active preset changed to: {}", presetName);
            return true;
        }

        logger.warn("Attempted to set non-existent preset as active: {}", presetName);
        return false;
    }

    public String getActivePresetName() {
        HeaderPreset activePreset = getActivePreset();
        return activePreset != null ? activePreset.getName() : null;
    }

    public String getDownstreamUrl() {
        return proximaProperties.getDownstream().getUrl();
    }

    public List<String> validateConfiguration() {
        return validator.validatePresets(proximaProperties.getPresets());
    }
}