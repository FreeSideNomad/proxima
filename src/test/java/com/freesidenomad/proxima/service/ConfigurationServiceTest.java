package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.validation.ConfigurationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private ProximaProperties proximaProperties;

    @Mock
    private ConfigurationValidator validator;

    @InjectMocks
    private ConfigurationService configurationService;

    private HeaderPreset adminPreset;
    private HeaderPreset userPreset;
    private List<HeaderPreset> presets;

    @BeforeEach
    void setUp() {
        adminPreset = new HeaderPreset();
        adminPreset.setName("admin_user");
        adminPreset.setDisplayName("Admin User");
        adminPreset.setHeaders(Map.of(
                "Authorization", "Bearer admin-token",
                "X-User-Role", "admin"
        ));

        userPreset = new HeaderPreset();
        userPreset.setName("regular_user");
        userPreset.setDisplayName("Regular User");
        userPreset.setHeaders(Map.of(
                "Authorization", "Bearer user-token",
                "X-User-Role", "user"
        ));

        presets = List.of(adminPreset, userPreset);
    }

    @Test
    void testGetAllPresets() {
        when(proximaProperties.getPresets()).thenReturn(presets);

        List<HeaderPreset> result = configurationService.getAllPresets();

        assertEquals(presets, result);
        verify(proximaProperties).getPresets();
    }

    @Test
    void testGetPresetByName() {
        when(proximaProperties.getPresets()).thenReturn(presets);

        Optional<HeaderPreset> result = configurationService.getPresetByName("admin_user");

        assertTrue(result.isPresent());
        assertEquals("admin_user", result.get().getName());
    }

    @Test
    void testGetPresetByNameNotFound() {
        when(proximaProperties.getPresets()).thenReturn(presets);

        Optional<HeaderPreset> result = configurationService.getPresetByName("non_existent");

        assertFalse(result.isPresent());
    }

    @Test
    void testGetActivePreset() {
        when(proximaProperties.getActivePreset()).thenReturn("admin_user");
        when(proximaProperties.getPresets()).thenReturn(presets);

        HeaderPreset result = configurationService.getActivePreset();

        assertNotNull(result);
        assertEquals("admin_user", result.getName());
    }

    @Test
    void testGetActivePresetFallbackToFirst() {
        when(proximaProperties.getActivePreset()).thenReturn(null);
        when(proximaProperties.getPresets()).thenReturn(presets);

        HeaderPreset result = configurationService.getActivePreset();

        assertNotNull(result);
        assertEquals("admin_user", result.getName());
    }

    @Test
    void testSetActivePreset() {
        when(proximaProperties.getPresets()).thenReturn(presets);

        boolean result = configurationService.setActivePreset("regular_user");

        assertTrue(result);
        verify(proximaProperties).setActivePreset("regular_user");
    }

    @Test
    void testSetActivePresetNotFound() {
        when(proximaProperties.getPresets()).thenReturn(presets);

        boolean result = configurationService.setActivePreset("non_existent");

        assertFalse(result);
        verify(proximaProperties, never()).setActivePreset(any());
    }

    @Test
    void testGetCurrentHeaders() {
        when(proximaProperties.getActivePreset()).thenReturn("admin_user");
        when(proximaProperties.getPresets()).thenReturn(presets);

        Map<String, String> result = configurationService.getCurrentHeaders();

        assertNotNull(result);
        assertEquals("Bearer admin-token", result.get("Authorization"));
        assertEquals("admin", result.get("X-User-Role"));
    }
}