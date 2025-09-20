package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.config.TestSecurityDisabledConfig;
import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.RouteRule;
import com.freesidenomad.proxima.service.ConfigurationService;
import com.freesidenomad.proxima.service.JwtService;
import com.freesidenomad.proxima.service.ProxyService;
import com.freesidenomad.proxima.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebUIController.class)
@Import(TestSecurityDisabledConfig.class)
class WebUIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigurationService configurationService;

    @MockBean
    private RouteService routeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private ProxyService proxyService;

    private HeaderPreset adminPreset;
    private RouteRule userRoute;

    @BeforeEach
    void setUp() {
        adminPreset = new HeaderPreset();
        adminPreset.setName("admin_user");
        adminPreset.setDisplayName("Admin User");
        adminPreset.setHeaders(Map.of(
                "Authorization", "Bearer admin-token",
                "X-User-Role", "admin"
        ));

        userRoute = new RouteRule();
        userRoute.setPathPattern("/api/users/**");
        userRoute.setTargetUrl("http://localhost:8081");
        userRoute.setDescription("User service");
        userRoute.setEnabled(true);
    }

    @Test
    void testDashboard() throws Exception {
        when(configurationService.getActivePresetName()).thenReturn("admin_user");
        when(configurationService.getDownstreamUrl()).thenReturn("http://httpbin.org");
        when(configurationService.getAllPresets()).thenReturn(List.of(adminPreset));
        when(configurationService.getCurrentHeaders()).thenReturn(adminPreset.getHeaders());

        mockMvc.perform(get("/proxima/ui"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("activePreset", "admin_user"))
                .andExpect(model().attribute("downstreamUrl", "http://httpbin.org"))
                .andExpect(model().attribute("totalPresets", 1));
    }

    @Test
    void testPresets() throws Exception {
        when(configurationService.getAllPresets()).thenReturn(List.of(adminPreset));
        when(configurationService.getActivePresetName()).thenReturn("admin_user");

        mockMvc.perform(get("/proxima/ui/presets"))
                .andExpect(status().isOk())
                .andExpect(view().name("presets"))
                .andExpect(model().attribute("activePreset", "admin_user"));
    }

    @Test
    void testPresetDetail() throws Exception {
        when(configurationService.getPresetByName("admin_user")).thenReturn(Optional.of(adminPreset));
        when(configurationService.getActivePresetName()).thenReturn("admin_user");

        mockMvc.perform(get("/proxima/ui/presets/admin_user"))
                .andExpect(status().isOk())
                .andExpect(view().name("preset-detail"))
                .andExpect(model().attribute("preset", adminPreset))
                .andExpect(model().attribute("isActive", true));
    }

    @Test
    void testPresetDetailNotFound() throws Exception {
        when(configurationService.getPresetByName("non_existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/proxima/ui/presets/non_existent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proxima/ui/presets"));
    }

    @Test
    void testHeaders() throws Exception {
        when(configurationService.getCurrentHeaders()).thenReturn(adminPreset.getHeaders());
        when(configurationService.getActivePresetName()).thenReturn("admin_user");

        mockMvc.perform(get("/proxima/ui/headers"))
                .andExpect(status().isOk())
                .andExpect(view().name("headers"))
                .andExpect(model().attribute("activePreset", "admin_user"));
    }

    @Test
    void testRoutes() throws Exception {
        when(routeService.getAllRoutes()).thenReturn(List.of(userRoute));
        when(routeService.hasRoutes()).thenReturn(true);
        when(routeService.getRouteCount()).thenReturn(1);
        when(routeService.getEnabledRouteCount()).thenReturn(1L);

        mockMvc.perform(get("/proxima/ui/routes"))
                .andExpect(status().isOk())
                .andExpect(view().name("routes"))
                .andExpect(model().attribute("hasRoutes", true))
                .andExpect(model().attribute("routeCount", 1));
    }

    @Test
    void testStatus() throws Exception {
        when(configurationService.getDownstreamUrl()).thenReturn("http://httpbin.org");
        when(configurationService.getActivePresetName()).thenReturn("admin_user");
        when(configurationService.validateConfiguration()).thenReturn(List.of());

        mockMvc.perform(get("/proxima/ui/status"))
                .andExpect(status().isOk())
                .andExpect(view().name("status"))
                .andExpect(model().attribute("activePreset", "admin_user"));
    }
}