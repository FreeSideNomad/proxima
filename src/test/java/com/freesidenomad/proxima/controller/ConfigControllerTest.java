package com.freesidenomad.proxima.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.service.JsonConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigController.class)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JsonConfigurationService jsonConfigurationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProximaConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new ProximaConfig();

        // Set up downstream
        ProximaConfig.Downstream downstream = new ProximaConfig.Downstream();
        downstream.setUrl("http://localhost:8081");
        testConfig.setDownstream(downstream);

        // Set up routes
        List<ProximaConfig.ConfigRoute> routes = new ArrayList<>();
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        // ConfigRoute doesn't have setName method, only pathPattern
        route.setPathPattern("/api/test/**");
        route.setTargetUrl("http://test.com");
        route.setDescription("Test route");
        route.setEnabled(true);
        routes.add(route);
        testConfig.setRoutes(routes);

        // Set up presets
        List<ProximaConfig.ConfigHeaderPreset> presets = new ArrayList<>();
        ProximaConfig.ConfigHeaderPreset preset = new ProximaConfig.ConfigHeaderPreset();
        preset.setName("test-preset");
        preset.setDisplayName("Test Preset");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        preset.setHeaders(headers);
        presets.add(preset);
        testConfig.setPresets(presets);
        testConfig.setActivePreset("test-preset");

        // Set up reserved routes
        List<String> reservedRoutes = new ArrayList<>();
        reservedRoutes.add("/proxima/**");
        reservedRoutes.add("/actuator/**");
        testConfig.setReservedRoutes(reservedRoutes);
    }

    @Test
    void testGetConfiguration_Success() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(get("/proxima/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downstream.url").value("http://localhost:8081"))
                .andExpect(jsonPath("$.routes[0].pathPattern").value("/api/test/**"))
                .andExpect(jsonPath("$.presets[0].name").value("test-preset"))
                .andExpect(jsonPath("$.activePreset").value("test-preset"));

        verify(jsonConfigurationService).loadConfiguration();
    }

    @Test
    void testGetConfiguration_Error() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(get("/proxima/api/config"))
                .andExpect(status().isInternalServerError());

        verify(jsonConfigurationService).loadConfiguration();
    }

    @Test
    void testUpdateConfiguration_Success() throws Exception {
        when(jsonConfigurationService.isValidRoute(anyString())).thenReturn(true);
        doNothing().when(jsonConfigurationService).saveConfiguration(any(ProximaConfig.class));

        mockMvc.perform(post("/proxima/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Configuration updated successfully"));

        verify(jsonConfigurationService).isValidRoute("/api/test/**");
        verify(jsonConfigurationService).saveConfiguration(any(ProximaConfig.class));
    }

    @Test
    void testUpdateConfiguration_InvalidRoute() throws Exception {
        when(jsonConfigurationService.isValidRoute(anyString())).thenReturn(false);

        mockMvc.perform(post("/proxima/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testConfig)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Route pattern conflicts with reserved routes: /api/test/**"));

        verify(jsonConfigurationService).isValidRoute("/api/test/**");
        verify(jsonConfigurationService, never()).saveConfiguration(any());
    }

    @Test
    void testUpdateConfiguration_SaveError() throws Exception {
        when(jsonConfigurationService.isValidRoute(anyString())).thenReturn(true);
        doThrow(new RuntimeException("Save error")).when(jsonConfigurationService).saveConfiguration(any());

        mockMvc.perform(post("/proxima/api/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testConfig)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to update configuration: Save error"));
    }

    @Test
    void testAddRoute_Success() throws Exception {
        ProximaConfig.ConfigRoute newRoute = new ProximaConfig.ConfigRoute();
        // ConfigRoute doesn't have setName method, only pathPattern
        newRoute.setPathPattern("/api/new/**");
        newRoute.setTargetUrl("http://new.com");
        newRoute.setDescription("New route");
        newRoute.setEnabled(true);

        when(jsonConfigurationService.isValidRoute("/api/new/**")).thenReturn(true);
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        mockMvc.perform(post("/proxima/api/config/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoute)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Route added successfully"));

        verify(jsonConfigurationService).isValidRoute("/api/new/**");
        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testAddRoute_InvalidRoute() throws Exception {
        ProximaConfig.ConfigRoute newRoute = new ProximaConfig.ConfigRoute();
        newRoute.setPathPattern("/proxima/admin/**");

        when(jsonConfigurationService.isValidRoute("/proxima/admin/**")).thenReturn(false);

        mockMvc.perform(post("/proxima/api/config/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoute)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Route pattern conflicts with reserved routes: /proxima/admin/**"));

        verify(jsonConfigurationService).isValidRoute("/proxima/admin/**");
        verify(jsonConfigurationService, never()).loadConfiguration();
    }

    @Test
    void testAddRoute_Error() throws Exception {
        ProximaConfig.ConfigRoute newRoute = new ProximaConfig.ConfigRoute();
        newRoute.setPathPattern("/api/error/**");

        when(jsonConfigurationService.isValidRoute(anyString())).thenReturn(true);
        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(post("/proxima/api/config/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newRoute)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to add route: Load error"));
    }

    @Test
    void testDeleteRoute_Success() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        mockMvc.perform(delete("/proxima/api/config/routes/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Route deleted successfully"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testDeleteRoute_InvalidIndex() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(delete("/proxima/api/config/routes/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid route index"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService, never()).saveConfiguration(any());
    }

    @Test
    void testDeleteRoute_NegativeIndex() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(delete("/proxima/api/config/routes/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid route index"));
    }

    @Test
    void testDeleteRoute_Error() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(delete("/proxima/api/config/routes/0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to delete route: Load error"));
    }

    @Test
    void testAddPreset_Success() throws Exception {
        ProximaConfig.ConfigHeaderPreset newPreset = new ProximaConfig.ConfigHeaderPreset();
        newPreset.setName("new-preset");
        newPreset.setDisplayName("New Preset");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom", "value");
        newPreset.setHeaders(headers);

        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        mockMvc.perform(post("/proxima/api/config/presets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPreset)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Preset added successfully"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testAddPreset_DuplicateName() throws Exception {
        ProximaConfig.ConfigHeaderPreset duplicatePreset = new ProximaConfig.ConfigHeaderPreset();
        duplicatePreset.setName("test-preset"); // Same name as existing preset

        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(post("/proxima/api/config/presets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicatePreset)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Preset name already exists: test-preset"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService, never()).saveConfiguration(any());
    }

    @Test
    void testAddPreset_Error() throws Exception {
        ProximaConfig.ConfigHeaderPreset newPreset = new ProximaConfig.ConfigHeaderPreset();
        newPreset.setName("error-preset");

        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(post("/proxima/api/config/presets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPreset)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to add preset: Load error"));
    }

    @Test
    void testDeletePreset_Success() throws Exception {
        // Create a config with multiple presets to test non-active preset deletion
        ProximaConfig multiPresetConfig = new ProximaConfig();
        List<ProximaConfig.ConfigHeaderPreset> presets = new ArrayList<>();

        ProximaConfig.ConfigHeaderPreset preset1 = new ProximaConfig.ConfigHeaderPreset();
        preset1.setName("preset1");
        preset1.setDisplayName("Preset 1");
        presets.add(preset1);

        ProximaConfig.ConfigHeaderPreset preset2 = new ProximaConfig.ConfigHeaderPreset();
        preset2.setName("preset2");
        preset2.setDisplayName("Preset 2");
        presets.add(preset2);

        multiPresetConfig.setPresets(presets);
        multiPresetConfig.setActivePreset("preset1");

        when(jsonConfigurationService.loadConfiguration()).thenReturn(multiPresetConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        // Delete the second preset (index 1)
        mockMvc.perform(delete("/proxima/api/config/presets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Preset deleted successfully"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testDeletePreset_ActivePreset() throws Exception {
        // Add another preset
        ProximaConfig.ConfigHeaderPreset secondPreset = new ProximaConfig.ConfigHeaderPreset();
        secondPreset.setName("second-preset");
        testConfig.getPresets().add(secondPreset);

        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        // Delete the active preset (index 0)
        mockMvc.perform(delete("/proxima/api/config/presets/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Preset deleted successfully"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testDeletePreset_LastActivePreset() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        // Delete the only preset (which is active)
        mockMvc.perform(delete("/proxima/api/config/presets/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Preset deleted successfully"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testDeletePreset_InvalidIndex() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(delete("/proxima/api/config/presets/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid preset index"));

        verify(jsonConfigurationService, never()).saveConfiguration(any());
    }

    @Test
    void testDeletePreset_Error() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(delete("/proxima/api/config/presets/0"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to delete preset: Load error"));
    }

    @Test
    void testSetActivePreset_Success() throws Exception {
        Map<String, String> request = Map.of("presetName", "test-preset");

        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);
        doNothing().when(jsonConfigurationService).saveConfiguration(any());

        mockMvc.perform(post("/proxima/api/config/active-preset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Active preset updated successfully"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService).saveConfiguration(any());
    }

    @Test
    void testSetActivePreset_EmptyName() throws Exception {
        Map<String, String> request = Map.of("presetName", "");

        mockMvc.perform(post("/proxima/api/config/active-preset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Preset name is required"));

        verify(jsonConfigurationService, never()).loadConfiguration();
    }

    @Test
    void testSetActivePreset_MissingName() throws Exception {
        Map<String, String> request = new HashMap<>();

        mockMvc.perform(post("/proxima/api/config/active-preset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Preset name is required"));
    }

    @Test
    void testSetActivePreset_NotFound() throws Exception {
        Map<String, String> request = Map.of("presetName", "non-existent-preset");

        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(post("/proxima/api/config/active-preset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Preset not found: non-existent-preset"));

        verify(jsonConfigurationService).loadConfiguration();
        verify(jsonConfigurationService, never()).saveConfiguration(any());
    }

    @Test
    void testSetActivePreset_Error() throws Exception {
        Map<String, String> request = Map.of("presetName", "test-preset");

        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(post("/proxima/api/config/active-preset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to set active preset: Load error"));
    }

    @Test
    void testGetReservedRoutes_Success() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(testConfig);

        mockMvc.perform(get("/proxima/api/config/reserved-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedRoutes[0]").value("/proxima/**"))
                .andExpect(jsonPath("$.reservedRoutes[1]").value("/actuator/**"));

        verify(jsonConfigurationService).loadConfiguration();
    }

    @Test
    void testGetReservedRoutes_Error() throws Exception {
        when(jsonConfigurationService.loadConfiguration()).thenThrow(new RuntimeException("Load error"));

        mockMvc.perform(get("/proxima/api/config/reserved-routes"))
                .andExpect(status().isInternalServerError());

        verify(jsonConfigurationService).loadConfiguration();
    }
}