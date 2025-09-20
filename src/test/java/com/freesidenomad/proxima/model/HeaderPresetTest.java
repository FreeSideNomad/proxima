package com.freesidenomad.proxima.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderPresetTest {

    @Test
    void testHeaderPreset_Creation() {
        HeaderPreset preset = new HeaderPreset();

        assertNull(preset.getName());
        assertNotNull(preset.getHeaders());
        assertTrue(preset.getHeaders().isEmpty());
    }

    @Test
    void testHeaderPreset_SettersAndGetters() {
        HeaderPreset preset = new HeaderPreset();

        preset.setName("test-preset");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("X-User-ID", "user123");
        preset.setHeaders(headers);

        assertEquals("test-preset", preset.getName());
        assertEquals(2, preset.getHeaders().size());
        assertEquals("Bearer token", preset.getHeaders().get("Authorization"));
        assertEquals("user123", preset.getHeaders().get("X-User-ID"));
    }

    @Test
    void testHeaderPreset_DefensiveCopyingGetHeaders() {
        HeaderPreset preset = new HeaderPreset();

        Map<String, String> originalHeaders = new HashMap<>();
        originalHeaders.put("X-Test", "value1");
        preset.setHeaders(originalHeaders);

        Map<String, String> returnedHeaders = preset.getHeaders();

        // Verify defensive copying - returned map should be different instance
        assertNotSame(originalHeaders, returnedHeaders);

        // Modify returned headers - should not affect preset's internal state
        returnedHeaders.put("X-Test", "modified");
        assertEquals("value1", preset.getHeaders().get("X-Test"));
    }

    @Test
    void testHeaderPreset_DefensiveCopyingSetHeaders() {
        HeaderPreset preset = new HeaderPreset();

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "value1");
        preset.setHeaders(headers);

        // Modify original map
        headers.put("X-Test", "modified");

        // Preset should still have original value due to defensive copying
        assertEquals("value1", preset.getHeaders().get("X-Test"));
    }

    @Test
    void testHeaderPreset_SetHeadersNull() {
        HeaderPreset preset = new HeaderPreset();

        preset.setHeaders(null);

        assertNotNull(preset.getHeaders());
        assertTrue(preset.getHeaders().isEmpty());
    }

    @Test
    void testHeaderPreset_EqualsAndHashCode() {
        HeaderPreset preset1 = new HeaderPreset();
        preset1.setName("test");

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "value");
        preset1.setHeaders(headers);

        HeaderPreset preset2 = new HeaderPreset();
        preset2.setName("test");
        preset2.setHeaders(new HashMap<>(headers));

        assertEquals(preset1, preset2);
        assertEquals(preset1.hashCode(), preset2.hashCode());
    }

    @Test
    void testHeaderPreset_NotEquals() {
        HeaderPreset preset1 = new HeaderPreset();
        preset1.setName("test1");

        HeaderPreset preset2 = new HeaderPreset();
        preset2.setName("test2");

        assertNotEquals(preset1, preset2);
    }

    @Test
    void testHeaderPreset_ToString() {
        HeaderPreset preset = new HeaderPreset();
        preset.setName("test-preset");

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "value");
        preset.setHeaders(headers);

        String toString = preset.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("test-preset"));
        assertTrue(toString.contains("X-Test"));
    }

    @Test
    void testHeaderPreset_CanEqual() {
        HeaderPreset preset1 = new HeaderPreset();
        HeaderPreset preset2 = new HeaderPreset();

        assertTrue(preset1.canEqual(preset2));
        assertFalse(preset1.canEqual("not a HeaderPreset"));
    }

    @Test
    void testHeaderPreset_EmptyHeaders() {
        HeaderPreset preset = new HeaderPreset();
        preset.setName("empty-preset");
        preset.setHeaders(new HashMap<>());

        assertEquals("empty-preset", preset.getName());
        assertTrue(preset.getHeaders().isEmpty());
    }

    @Test
    void testHeaderPreset_MultipleHeaders() {
        HeaderPreset preset = new HeaderPreset();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("X-User-ID", "user456");
        headers.put("X-API-Key", "api789");
        headers.put("Content-Type", "application/json");
        preset.setHeaders(headers);

        assertEquals(4, preset.getHeaders().size());
        assertEquals("Bearer token123", preset.getHeaders().get("Authorization"));
        assertEquals("user456", preset.getHeaders().get("X-User-ID"));
        assertEquals("api789", preset.getHeaders().get("X-API-Key"));
        assertEquals("application/json", preset.getHeaders().get("Content-Type"));
    }
}