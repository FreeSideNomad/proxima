package com.freesidenomad.proxima.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DevModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testDashboardShowsCorrectDownstreamUrl() throws Exception {
        // Test that the dashboard shows the test configuration URL (localhost:9999)
        mockMvc.perform(get("/proxima/ui/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:9999")));
    }

    @Test
    void testRoutesPageShowsNoRoutesConfigured() throws Exception {
        // Test that routes page shows no routes (test config has empty routes array)
        mockMvc.perform(get("/proxima/ui/routes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("NO ROUTES CONFIGURED")));
    }

    @Test
    void testJwtPageLoadsWithoutError() throws Exception {
        // Test that JWT page loads successfully without throwing errors
        mockMvc.perform(get("/proxima/ui/jwt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JWT Token Generation")));
    }

    @Test
    void testSwaggerUIAccessible() throws Exception {
        // Test that Swagger UI is accessible at the correct path (expects redirect to swagger-ui/index.html)
        mockMvc.perform(get("/proxima/api/docs"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testSwaggerApiDocsAccessible() throws Exception {
        // Test that API docs JSON is accessible
        mockMvc.perform(get("/proxima/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testIncorrectSwaggerPathReturns200() throws Exception {
        // Test that the swagger-ui path actually works correctly
        mockMvc.perform(get("/proxima/api/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}