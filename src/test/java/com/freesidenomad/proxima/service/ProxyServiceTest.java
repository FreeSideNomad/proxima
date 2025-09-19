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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private RouteService routeService;

    @InjectMocks
    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        headers.put("X-User-Role", "admin");

        Map<String, String> headerMappings = new HashMap<>();

        lenient().when(configurationService.getCurrentHeaders()).thenReturn(headers);
        lenient().when(configurationService.getActivePresetName()).thenReturn("admin_user");
        lenient().when(configurationService.getActiveHeaderMappings()).thenReturn(headerMappings);
    }

    @Test
    void testForwardRequestBuildsCorrectTargetUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Type", "application/json");
        request.setRemoteAddr("127.0.0.1");

        String method = "GET";
        String path = "/api/test";
        String expectedUrl = "http://test-server.com/api/test";

        when(routeService.resolveTargetUrl(path)).thenReturn(expectedUrl);

        CompletableFuture<ResponseEntity<String>> result =
            proxyService.forwardRequest(method, path, request, null);

        assertNotNull(result);
        verify(routeService).resolveTargetUrl(path);
    }

    @Test
    void testForwardRequestReservedRoute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String method = "GET";
        String path = "/proxima/api/config";

        when(routeService.resolveTargetUrl(path)).thenReturn(null);

        CompletableFuture<ResponseEntity<String>> result =
            proxyService.forwardRequest(method, path, request, null);

        assertNotNull(result);
        ResponseEntity<String> response = result.join();
        assertEquals(404, response.getStatusCodeValue());
    }
}