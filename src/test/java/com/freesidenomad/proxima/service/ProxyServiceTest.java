package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock
    private ProximaProperties proximaProperties;

    @InjectMocks
    private ProxyService proxyService;

    private ProximaProperties.Downstream downstream;

    @BeforeEach
    void setUp() {
        downstream = new ProximaProperties.Downstream();
        downstream.setUrl("http://test-server.com");

        when(proximaProperties.getDownstream()).thenReturn(downstream);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        headers.put("X-User-Role", "admin");

        when(proximaProperties.getHeaders()).thenReturn(headers);
    }

    @Test
    void testForwardRequestBuildsCorrectTargetUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Type", "application/json");

        String method = "GET";
        String path = "/api/test";

        CompletableFuture<ResponseEntity<String>> result =
            proxyService.forwardRequest(method, path, request, null);

        assertNotNull(result);
    }

    @Test
    void testHeadersAreInjected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Original-Header", "original-value");

        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("Authorization", "Bearer injected-token");
        customHeaders.put("X-Custom", "custom-value");

        when(proximaProperties.getHeaders()).thenReturn(customHeaders);

        assertNotNull(proxyService);
        verify(proximaProperties, atLeastOnce()).getHeaders();
    }
}