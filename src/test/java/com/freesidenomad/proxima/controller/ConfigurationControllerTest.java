package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.service.ConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigurationController.class)
class ConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigurationService configurationService;

    @Autowired
    private ObjectMapper objectMapper;

    private HeaderPreset adminPreset;

    @BeforeEach
    void setUp() {
        adminPreset = new HeaderPreset();
        adminPreset.setName("admin_user");
        adminPreset.setDisplayName("Admin User");
        adminPreset.setHeaders(Map.of(
                "Authorization", "Bearer admin-token",
                "X-User-Role", "admin"
        ));
    }

    @Test
    void testGetAllPresets() throws Exception {
        when(configurationService.getAllPresets()).thenReturn(List.of(adminPreset));

        mockMvc.perform(get("/api/config/presets"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("admin_user"))
                .andExpect(jsonPath("$[0].displayName").value("Admin User"));
    }

    @Test
    void testGetPreset() throws Exception {
        when(configurationService.getPresetByName("admin_user")).thenReturn(Optional.of(adminPreset));

        mockMvc.perform(get("/api/config/presets/admin_user"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("admin_user"))
                .andExpect(jsonPath("$.displayName").value("Admin User"));
    }

    @Test
    void testGetPresetNotFound() throws Exception {
        when(configurationService.getPresetByName("non_existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/config/presets/non_existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetActivePreset() throws Exception {
        when(configurationService.getActivePreset()).thenReturn(adminPreset);
        when(configurationService.getActivePresetName()).thenReturn("admin_user");

        mockMvc.perform(get("/api/config/active-preset"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("admin_user"))
                .andExpect(jsonPath("$.preset.name").value("admin_user"));
    }

    @Test
    void testActivatePreset() throws Exception {
        when(configurationService.setActivePreset("admin_user")).thenReturn(true);

        mockMvc.perform(post("/api/config/presets/admin_user/activate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.activePreset").value("admin_user"));
    }

    @Test
    void testActivatePresetNotFound() throws Exception {
        when(configurationService.setActivePreset("non_existent")).thenReturn(false);

        mockMvc.perform(post("/api/config/presets/non_existent/activate"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testGetCurrentHeaders() throws Exception {
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer test-token",
                "X-User-Role", "admin"
        );
        when(configurationService.getCurrentHeaders()).thenReturn(headers);

        mockMvc.perform(get("/api/config/headers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Authorization").value("Bearer test-token"))
                .andExpect(jsonPath("$.['X-User-Role']").value("admin"));
    }

    @Test
    void testGetConfigInfo() throws Exception {
        when(configurationService.getDownstreamUrl()).thenReturn("http://test-server.com");
        when(configurationService.getActivePresetName()).thenReturn("admin_user");
        when(configurationService.getAllPresets()).thenReturn(List.of(adminPreset));

        mockMvc.perform(get("/api/config/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.downstreamUrl").value("http://test-server.com"))
                .andExpect(jsonPath("$.activePreset").value("admin_user"))
                .andExpect(jsonPath("$.totalPresets").value(1));
    }

    @Test
    void testValidateConfiguration() throws Exception {
        when(configurationService.validateConfiguration()).thenReturn(List.of());

        mockMvc.perform(get("/api/config/validate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());
    }
}