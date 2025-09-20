package com.freesidenomad.proxima.config;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.RouteRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProximaPropertiesTest {

    @Test
    void testProximaProperties_Creation() {
        ProximaProperties properties = new ProximaProperties();

        assertNotNull(properties.getDownstream());
        assertNotNull(properties.getHeaders());
        assertNotNull(properties.getPresets());
        assertNotNull(properties.getRoutes());
        assertTrue(properties.getHeaders().isEmpty());
        assertTrue(properties.getPresets().isEmpty());
        assertTrue(properties.getRoutes().isEmpty());
    }

    @Test
    void testDownstream_SettersAndGetters() {
        ProximaProperties properties = new ProximaProperties();

        ProximaProperties.Downstream downstream = new ProximaProperties.Downstream();
        downstream.setUrl("http://localhost:8080");

        properties.setDownstream(downstream);

        ProximaProperties.Downstream result = properties.getDownstream();
        assertNotNull(result);
        assertEquals("http://localhost:8080", result.getUrl());

        // Verify defensive copying
        assertNotSame(downstream, result);
    }

    @Test
    void testDownstream_SetNull() {
        ProximaProperties properties = new ProximaProperties();

        properties.setDownstream(null);

        assertNull(properties.getDownstream());
    }

    @Test
    void testDownstream_GetNull() {
        ProximaProperties properties = new ProximaProperties();

        // Set downstream to null directly (bypass setter for test)
        properties.setDownstream(null);

        assertNull(properties.getDownstream());
    }

    @Test
    void testHeaders_SettersAndGetters() {
        ProximaProperties properties = new ProximaProperties();

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "value1");
        headers.put("Authorization", "Bearer token");

        properties.setHeaders(headers);

        Map<String, String> result = properties.getHeaders();
        assertEquals(2, result.size());
        assertEquals("value1", result.get("X-Test"));
        assertEquals("Bearer token", result.get("Authorization"));

        // Verify defensive copying
        assertNotSame(headers, result);

        // Modify original map
        headers.put("X-Test", "modified");

        // Result should not be affected
        assertEquals("value1", properties.getHeaders().get("X-Test"));
    }

    @Test
    void testHeaders_SetNull() {
        ProximaProperties properties = new ProximaProperties();

        properties.setHeaders(null);

        assertNotNull(properties.getHeaders());
        assertTrue(properties.getHeaders().isEmpty());
    }

    @Test
    void testPresets_SettersAndGetters() {
        ProximaProperties properties = new ProximaProperties();

        List<HeaderPreset> presets = new ArrayList<>();
        HeaderPreset preset1 = new HeaderPreset();
        preset1.setName("preset1");
        presets.add(preset1);

        properties.setPresets(presets);

        List<HeaderPreset> result = properties.getPresets();
        assertEquals(1, result.size());
        assertEquals("preset1", result.get(0).getName());

        // Verify defensive copying
        assertNotSame(presets, result);

        // Modify original list
        HeaderPreset preset2 = new HeaderPreset();
        preset2.setName("preset2");
        presets.add(preset2);

        // Result should not be affected
        assertEquals(1, properties.getPresets().size());
    }

    @Test
    void testPresets_SetNull() {
        ProximaProperties properties = new ProximaProperties();

        properties.setPresets(null);

        assertNotNull(properties.getPresets());
        assertTrue(properties.getPresets().isEmpty());
    }

    @Test
    void testActivePreset_SettersAndGetters() {
        ProximaProperties properties = new ProximaProperties();

        properties.setActivePreset("test-preset");

        assertEquals("test-preset", properties.getActivePreset());
    }

    @Test
    void testRoutes_SettersAndGetters() {
        ProximaProperties properties = new ProximaProperties();

        List<RouteRule> routes = new ArrayList<>();
        RouteRule route1 = new RouteRule();
        route1.setPathPattern("/api/test");
        routes.add(route1);

        properties.setRoutes(routes);

        List<RouteRule> result = properties.getRoutes();
        assertEquals(1, result.size());
        assertEquals("/api/test", result.get(0).getPathPattern());

        // Verify defensive copying
        assertNotSame(routes, result);

        // Modify original list
        RouteRule route2 = new RouteRule();
        route2.setPathPattern("/api/test2");
        routes.add(route2);

        // Result should not be affected
        assertEquals(1, properties.getRoutes().size());
    }

    @Test
    void testRoutes_SetNull() {
        ProximaProperties properties = new ProximaProperties();

        properties.setRoutes(null);

        assertNotNull(properties.getRoutes());
        assertTrue(properties.getRoutes().isEmpty());
    }

    // Test the inner Downstream class
    @Test
    void testDownstream_Creation() {
        ProximaProperties.Downstream downstream = new ProximaProperties.Downstream();

        assertNotNull(downstream);
        assertNull(downstream.getUrl());
    }

    @Test
    void testDownstream_SettersAndGettersIndependent() {
        ProximaProperties.Downstream downstream = new ProximaProperties.Downstream();

        downstream.setUrl("http://example.com");

        assertEquals("http://example.com", downstream.getUrl());
    }

    @Test
    void testDownstream_EqualsAndHashCode() {
        ProximaProperties.Downstream downstream1 = new ProximaProperties.Downstream();
        downstream1.setUrl("http://test.com");

        ProximaProperties.Downstream downstream2 = new ProximaProperties.Downstream();
        downstream2.setUrl("http://test.com");

        assertEquals(downstream1, downstream2);
        assertEquals(downstream1.hashCode(), downstream2.hashCode());
    }

    @Test
    void testDownstream_NotEquals() {
        ProximaProperties.Downstream downstream1 = new ProximaProperties.Downstream();
        downstream1.setUrl("http://test1.com");

        ProximaProperties.Downstream downstream2 = new ProximaProperties.Downstream();
        downstream2.setUrl("http://test2.com");

        assertNotEquals(downstream1, downstream2);
    }

    @Test
    void testDownstream_ToString() {
        ProximaProperties.Downstream downstream = new ProximaProperties.Downstream();
        downstream.setUrl("http://test.com");

        String toString = downstream.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("http://test.com"));
    }
}