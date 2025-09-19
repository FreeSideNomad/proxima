package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.ProximaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class HeaderMappingUnitTest {

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private RouteService routeService;

    @InjectMocks
    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        // Set up a preset with header mappings similar to our config
        Map<String, String> presetHeaders = new HashMap<>();
        presetHeaders.put("Authorization", "Bearer preset-token");
        presetHeaders.put("X-User-Role", "admin");

        Map<String, String> headerMappings = new HashMap<>();
        headerMappings.put("Authorization", "Original-Auth");
        headerMappings.put("User-Agent", "Source-User-Agent");

        lenient().when(configurationService.getCurrentHeaders()).thenReturn(presetHeaders);
        lenient().when(configurationService.getActiveHeaderMappings()).thenReturn(headerMappings);
        lenient().when(routeService.resolveTargetUrl(anyString())).thenReturn("http://test-server.com/api");
    }

    @Test
    void testHeaderMappingWithAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer incoming-token");
        request.addHeader("Content-Type", "application/json");

        // Use reflection to access the buildHeaders method
        try {
            var method = ProxyService.class.getDeclaredMethod("buildHeaders", jakarta.servlet.http.HttpServletRequest.class);
            method.setAccessible(true);
            HttpHeaders result = (HttpHeaders) method.invoke(proxyService, request);

            // Verify header mapping: incoming Authorization should be mapped to Original-Auth
            assertEquals("Bearer incoming-token", result.getFirst("Original-Auth"));

            // Verify preset headers are added
            assertEquals("Bearer preset-token", result.getFirst("Authorization"));
            assertEquals("admin", result.getFirst("X-User-Role"));

            // Verify unmapped headers pass through
            assertEquals("application/json", result.getFirst("Content-Type"));

        } catch (Exception e) {
            fail("Failed to test buildHeaders method: " + e.getMessage());
        }
    }

    @Test
    void testHeaderMappingWithUserAgentHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "MyApp/1.0");
        request.addHeader("Authorization", "Bearer test-token");

        try {
            var method = ProxyService.class.getDeclaredMethod("buildHeaders", jakarta.servlet.http.HttpServletRequest.class);
            method.setAccessible(true);
            HttpHeaders result = (HttpHeaders) method.invoke(proxyService, request);

            // Verify header mappings
            assertEquals("Bearer test-token", result.getFirst("Original-Auth"));
            assertEquals("MyApp/1.0", result.getFirst("Source-User-Agent"));

            // Verify preset headers
            assertEquals("Bearer preset-token", result.getFirst("Authorization"));
            assertEquals("admin", result.getFirst("X-User-Role"));

        } catch (Exception e) {
            fail("Failed to test buildHeaders method: " + e.getMessage());
        }
    }

    @Test
    void testNoHeaderMappingWhenMappingsEmpty() {
        // Set up empty header mappings
        when(configurationService.getActiveHeaderMappings()).thenReturn(new HashMap<>());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        request.addHeader("User-Agent", "TestApp/1.0");

        try {
            var method = ProxyService.class.getDeclaredMethod("buildHeaders", jakarta.servlet.http.HttpServletRequest.class);
            method.setAccessible(true);
            HttpHeaders result = (HttpHeaders) method.invoke(proxyService, request);

            // With no mappings, incoming headers should pass through unchanged
            // But Authorization will be overridden by preset
            assertEquals("Bearer preset-token", result.getFirst("Authorization"));
            assertEquals("TestApp/1.0", result.getFirst("User-Agent"));

            // Should not have any mapped headers
            assertNull(result.getFirst("Original-Auth"));
            assertNull(result.getFirst("Source-User-Agent"));

        } catch (Exception e) {
            fail("Failed to test buildHeaders method: " + e.getMessage());
        }
    }

    @Test
    void testHopByHopHeadersAreFiltered() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Host", "example.com");
        request.addHeader("Authorization", "Bearer test-token");

        try {
            var method = ProxyService.class.getDeclaredMethod("buildHeaders", jakarta.servlet.http.HttpServletRequest.class);
            method.setAccessible(true);
            HttpHeaders result = (HttpHeaders) method.invoke(proxyService, request);

            // Hop-by-hop headers should be filtered out
            assertNull(result.getFirst("Connection"));
            assertNull(result.getFirst("Host"));

            // Other headers should be processed normally
            assertEquals("Bearer test-token", result.getFirst("Original-Auth"));
            assertEquals("Bearer preset-token", result.getFirst("Authorization"));

        } catch (Exception e) {
            fail("Failed to test buildHeaders method: " + e.getMessage());
        }
    }
}