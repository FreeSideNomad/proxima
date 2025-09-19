package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.validation.ConfigurationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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
    private JsonConfigurationService jsonConfigurationService;

    @Mock
    private ConfigurationValidator validator;

    @InjectMocks
    private ConfigurationService configurationService;

    private HeaderPreset adminPreset;
    private HeaderPreset userPreset;
    private List<HeaderPreset> presets;
    private ProximaConfig mockConfig;

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

        // Create mock config
        mockConfig = new ProximaConfig();
        mockConfig.setActivePreset("admin_user");

        List<ProximaConfig.ConfigHeaderPreset> configPresets = new ArrayList<>();

        ProximaConfig.ConfigHeaderPreset adminConfigPreset = new ProximaConfig.ConfigHeaderPreset();
        adminConfigPreset.setName("admin_user");
        adminConfigPreset.setDisplayName("Admin User");
        adminConfigPreset.setHeaders(Map.of(
                "Authorization", "Bearer admin-token",
                "X-User-Role", "admin"
        ));
        configPresets.add(adminConfigPreset);

        ProximaConfig.ConfigHeaderPreset userConfigPreset = new ProximaConfig.ConfigHeaderPreset();
        userConfigPreset.setName("regular_user");
        userConfigPreset.setDisplayName("Regular User");
        userConfigPreset.setHeaders(Map.of(
                "Authorization", "Bearer user-token",
                "X-User-Role", "user"
        ));
        configPresets.add(userConfigPreset);

        mockConfig.setPresets(configPresets);
    }

    @Test
    void testGetAllPresets() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        List<HeaderPreset> result = configurationService.getAllPresets();

        assertEquals(2, result.size());
        assertEquals("admin_user", result.get(0).getName());
        assertEquals("regular_user", result.get(1).getName());
        verify(jsonConfigurationService).loadConfiguration();
    }

    @Test
    void testGetPresetByName() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        Optional<HeaderPreset> result = configurationService.getPresetByName("admin_user");

        assertTrue(result.isPresent());
        assertEquals("admin_user", result.get().getName());
    }

    @Test
    void testGetPresetByNameNotFound() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        Optional<HeaderPreset> result = configurationService.getPresetByName("non_existent");

        assertFalse(result.isPresent());
    }

    @Test
    void testGetActivePreset() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        HeaderPreset result = configurationService.getActivePreset();

        assertNotNull(result);
        assertEquals("admin_user", result.getName());
    }

    @Test
    void testGetActivePresetFallbackToFirst() {
        mockConfig.setActivePreset(null);
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        HeaderPreset result = configurationService.getActivePreset();

        assertNotNull(result);
        assertEquals("admin_user", result.getName());
    }

    @Test
    void testSetActivePreset() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        boolean result = configurationService.setActivePreset("regular_user");

        assertTrue(result);
        verify(jsonConfigurationService).saveConfiguration(any(ProximaConfig.class));
    }

    @Test
    void testSetActivePresetNotFound() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        boolean result = configurationService.setActivePreset("non_existent");

        assertFalse(result);
        verify(jsonConfigurationService, never()).saveConfiguration(any());
    }

    @Test
    void testGetCurrentHeaders() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        Map<String, String> result = configurationService.getCurrentHeaders();

        assertNotNull(result);
        assertEquals("Bearer admin-token", result.get("Authorization"));
        assertEquals("admin", result.get("X-User-Role"));
    }
}