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
        mockWebServer.start(8081);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testHeaderMappingWithDefaultActivePreset() throws Exception {
        // The current active preset is "regular_user" which has header mappings:
        // "Authorization": "Original-Auth"
        // This means that if a request comes in with "Authorization" header,
        // it should be remapped to "Original-Auth" header in the downstream request

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"success\"}"));

        mockMvc.perform(get("/api/users/123")
                .header("Authorization", "Bearer incoming-token")
                .header("User-Agent", "TestAgent/1.0"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("/123", recordedRequest.getPath());

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
}