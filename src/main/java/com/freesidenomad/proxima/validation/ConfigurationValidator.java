package com.freesidenomad.proxima.validation;

import com.freesidenomad.proxima.model.HeaderPreset;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConfigurationValidator {

    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "host", "connection", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade"
    );

    public List<String> validatePresets(List<HeaderPreset> presets) {
        List<String> errors = new ArrayList<>();

        if (presets == null || presets.isEmpty()) {
            return errors;
        }

        Set<String> presetNames = presets.stream()
                .map(HeaderPreset::getName)
                .collect(Collectors.toSet());

        if (presetNames.size() != presets.size()) {
            errors.add("Duplicate preset names found");
        }

        for (HeaderPreset preset : presets) {
            errors.addAll(validatePreset(preset));
        }

        return errors;
    }

    public List<String> validatePreset(HeaderPreset preset) {
        List<String> errors = new ArrayList<>();

        if (preset == null) {
            errors.add("Preset cannot be null");
            return errors;
        }

        if (preset.getName() == null || preset.getName().trim().isEmpty()) {
            errors.add("Preset name cannot be empty");
        } else if (!isValidPresetName(preset.getName())) {
            errors.add("Preset name '" + preset.getName() + "' contains invalid characters");
        }

        if (preset.getDisplayName() == null || preset.getDisplayName().trim().isEmpty()) {
            errors.add("Preset display name cannot be empty");
        }

        if (preset.getHeaders() != null) {
            for (String headerName : preset.getHeaders().keySet()) {
                if (FORBIDDEN_HEADERS.contains(headerName.toLowerCase(java.util.Locale.ENGLISH))) {
                    errors.add("Header '" + headerName + "' is not allowed in preset '" + preset.getName() + "'");
                }

                if (!isValidHeaderName(headerName)) {
                    errors.add("Invalid header name '" + headerName + "' in preset '" + preset.getName() + "'");
                }
            }
        }

        return errors;
    }

    private boolean isValidPresetName(String name) {
        return name.matches("^[a-zA-Z0-9_-]+$");
    }

    private boolean isValidHeaderName(String headerName) {
        return headerName != null &&
               !headerName.trim().isEmpty() &&
               headerName.matches("^[a-zA-Z0-9-_]+$");
    }
}