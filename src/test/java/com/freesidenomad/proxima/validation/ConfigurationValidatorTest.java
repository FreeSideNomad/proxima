package com.freesidenomad.proxima.validation;

import com.freesidenomad.proxima.model.HeaderPreset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationValidatorTest {

    private ConfigurationValidator validator;
    private HeaderPreset validPreset;

    @BeforeEach
    void setUp() {
        validator = new ConfigurationValidator();

        validPreset = new HeaderPreset();
        validPreset.setName("admin_user");
        validPreset.setDisplayName("Admin User");
        validPreset.setHeaders(Map.of(
                "Authorization", "Bearer token",
                "X-User-Role", "admin"
        ));
    }

    @Test
    void testValidateValidPreset() {
        List<String> errors = validator.validatePreset(validPreset);

        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateNullPreset() {
        List<String> errors = validator.validatePreset(null);

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("cannot be null"));
    }

    @Test
    void testValidateEmptyPresetName() {
        validPreset.setName("");

        List<String> errors = validator.validatePreset(validPreset);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("name cannot be empty")));
    }

    @Test
    void testValidateInvalidPresetName() {
        validPreset.setName("invalid name with spaces");

        List<String> errors = validator.validatePreset(validPreset);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("invalid characters")));
    }

    @Test
    void testValidateEmptyDisplayName() {
        validPreset.setDisplayName("");

        List<String> errors = validator.validatePreset(validPreset);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("display name cannot be empty")));
    }

    @Test
    void testValidateForbiddenHeader() {
        validPreset.setHeaders(Map.of(
                "Authorization", "Bearer token",
                "host", "forbidden-header"
        ));

        List<String> errors = validator.validatePreset(validPreset);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("not allowed")));
    }

    @Test
    void testValidateInvalidHeaderName() {
        validPreset.setHeaders(Map.of(
                "Authorization", "Bearer token",
                "Invalid Header Name", "value"
        ));

        List<String> errors = validator.validatePreset(validPreset);

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("Invalid header name")));
    }

    @Test
    void testValidateMultiplePresets() {
        HeaderPreset preset1 = new HeaderPreset();
        preset1.setName("preset1");
        preset1.setDisplayName("Preset 1");
        preset1.setHeaders(Map.of("X-Test", "value1"));

        HeaderPreset preset2 = new HeaderPreset();
        preset2.setName("preset2");
        preset2.setDisplayName("Preset 2");
        preset2.setHeaders(Map.of("X-Test", "value2"));

        List<String> errors = validator.validatePresets(List.of(preset1, preset2));

        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateDuplicatePresetNames() {
        HeaderPreset preset1 = new HeaderPreset();
        preset1.setName("duplicate");
        preset1.setDisplayName("Preset 1");

        HeaderPreset preset2 = new HeaderPreset();
        preset2.setName("duplicate");
        preset2.setDisplayName("Preset 2");

        List<String> errors = validator.validatePresets(List.of(preset1, preset2));

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("Duplicate preset names")));
    }
}