package com.freesidenomad.proxima.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HeaderMappingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(9999);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testHeaderMappingWithCurrentActivePreset() throws Exception {
        // The current active preset is "regular_user" which has header mappings:
        // "Authorization": "Original-Auth"
        // This means that if a request comes in with "Authorization" header,
        // it should be remapped to "Original-Auth" header in the downstream request

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"success\"}"));

        var mvcResult = mockMvc.perform(get("/test/mapping")
                .header("Authorization", "Bearer incoming-token")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/test/mapping", recordedRequest.getPath());

        // Check that the Authorization header from the request was remapped to Original-Auth
        assertEquals("Bearer incoming-token", recordedRequest.getHeader("Original-Auth"));

        // Check that User-Agent header was passed through unchanged (no mapping configured)
        assertEquals("TestAgent/1.0", recordedRequest.getHeader("User-Agent"));

        // Check that preset headers were added
        assertEquals("Bearer user-jwt-token", recordedRequest.getHeader("Authorization"));
        assertEquals("user", recordedRequest.getHeader("X-User-Role"));
        assertEquals("user-001", recordedRequest.getHeader("X-User-ID"));
        assertEquals("Proxima-Proxy", recordedRequest.getHeader("X-Forwarded-By"));
    }

    @Test
    void testHeaderMappingWithAdminPreset() throws Exception {
        // First, switch to admin_user preset which has different header mappings:
        // "Authorization": "Source-Authorization"
        // "User-Agent": "Source-User-Agent"

        // Switch to admin preset
        mockMvc.perform(post("/proxima/api/config/presets/admin_user/activate"))
                .andExpect(status().isOk());

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"success\"}"));

        var mvcResult = mockMvc.perform(get("/test/admin-mapping")
                .header("Authorization", "Bearer admin-incoming-token")
                .header("User-Agent", "AdminAgent/2.0")
                .header("Custom-Header", "custom-value"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/test/admin-mapping", recordedRequest.getPath());

        // Check that headers were remapped according to admin preset
        assertEquals("Bearer admin-incoming-token", recordedRequest.getHeader("Source-Authorization"));
        assertEquals("AdminAgent/2.0", recordedRequest.getHeader("Source-User-Agent"));

        // Check that unmapped headers pass through unchanged
        assertEquals("custom-value", recordedRequest.getHeader("Custom-Header"));

        // Check that preset headers were added (admin preset headers)
        assertEquals("Bearer admin-jwt-token", recordedRequest.getHeader("Authorization"));
        assertEquals("admin", recordedRequest.getHeader("X-User-Role"));
        assertEquals("admin-001", recordedRequest.getHeader("X-User-ID"));
        assertEquals("Proxima-Proxy", recordedRequest.getHeader("X-Forwarded-By"));
    }

    @Test
    void testNoHeaderMappingWithApiClientPreset() throws Exception {
        // Switch to api_client preset which has no header mappings

        // Switch to api_client preset
        mockMvc.perform(post("/proxima/api/config/presets/api_client/activate"))
                .andExpect(status().isOk());

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"success\"}"));

        var mvcResult = mockMvc.perform(get("/test/no-mapping")
                .header("Authorization", "Bearer api-incoming-token")
                .header("User-Agent", "ApiClient/1.0"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/test/no-mapping", recordedRequest.getPath());

        // Since api_client has no header mappings, incoming headers should be overridden by preset headers
        // The incoming Authorization header should be completely replaced by the preset Authorization
        assertEquals("Bearer api-client-token", recordedRequest.getHeader("Authorization"));

        // User-Agent should pass through unchanged since no mapping and no preset override
        assertEquals("ApiClient/1.0", recordedRequest.getHeader("User-Agent"));

        // Check preset headers
        assertEquals("api", recordedRequest.getHeader("X-Client-Type"));
        assertEquals("1000", recordedRequest.getHeader("X-Rate-Limit"));
        assertEquals("Proxima-Proxy", recordedRequest.getHeader("X-Forwarded-By"));
    }
}