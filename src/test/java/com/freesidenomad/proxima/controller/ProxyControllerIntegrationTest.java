package com.freesidenomad.proxima.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "proxima.downstream.url=http://localhost:9999",
    "proxima.headers.authorization=Bearer test-jwt-token",
    "proxima.headers.x-user-role=test-user"
})
class ProxyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().port(9999));
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testProxyGetRequest() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\"}")));

        mockMvc.perform(get("/proxy/api/test"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\"}"));

        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/test"))
                .withHeader("Authorization", equalTo("Bearer test-jwt-token"))
                .withHeader("X-User-Role", equalTo("test-user")));
    }

    @Test
    void testProxyPostRequest() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":123}")));

        mockMvc.perform(post("/proxy/api/create")
                .contentType("application/json")
                .content("{\"name\":\"test\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"id\":123}"));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/create"))
                .withHeader("Authorization", equalTo("Bearer test-jwt-token"))
                .withHeader("X-User-Role", equalTo("test-user"))
                .withRequestBody(equalTo("{\"name\":\"test\"}")));
    }
}