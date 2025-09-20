package com.freesidenomad.proxima.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProxyControllerIntegrationTest {

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
    void testProxyGetRequest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"success\"}"));

        mockMvc.perform(get("/api/users/123"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/123", recordedRequest.getPath());
        assertEquals("Bearer user-jwt-token", recordedRequest.getHeader("Authorization"));
        assertEquals("user", recordedRequest.getHeader("X-User-Role"));
    }

    @Test
    void testProxyPostRequest() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":123}"));

        mockMvc.perform(post("/api/users")
                .contentType("application/json")
                .content("{\"name\":\"test\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"id\":123}"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/", recordedRequest.getPath());
        assertEquals("Bearer user-jwt-token", recordedRequest.getHeader("Authorization"));
        assertEquals("user", recordedRequest.getHeader("X-User-Role"));
        assertEquals("{\"name\":\"test\"}", recordedRequest.getBody().readUtf8());
    }
}