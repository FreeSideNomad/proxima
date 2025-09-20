package com.freesidenomad.proxima.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRouteTest {

    @Test
    void testConfigRoute_Creation() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();

        assertNull(route.getPathPattern());
        assertNull(route.getTargetUrl());
        assertNull(route.getDescription());
        assertTrue(route.isEnabled());
    }

    @Test
    void testConfigRoute_SettersAndGetters() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();

        route.setPathPattern("/api/users/**");
        route.setTargetUrl("http://user-service.com");
        route.setDescription("User service routing");
        route.setEnabled(false);

        assertEquals("/api/users/**", route.getPathPattern());
        assertEquals("http://user-service.com", route.getTargetUrl());
        assertEquals("User service routing", route.getDescription());
        assertFalse(route.isEnabled());
    }

    @Test
    void testMatches_WildcardPattern() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/users/**");
        route.setEnabled(true);

        assertTrue(route.matches("/api/users/"));
        assertTrue(route.matches("/api/users/123"));
        assertTrue(route.matches("/api/users/123/profile"));
        assertFalse(route.matches("/api/orders/456"));
        assertFalse(route.matches("/api"));
    }

    @Test
    void testMatches_SingleWildcardPattern() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/users/*");
        route.setEnabled(true);

        assertTrue(route.matches("/api/users/123"));
        assertTrue(route.matches("/api/users/"));
        assertFalse(route.matches("/api/users/123/profile"));
        assertFalse(route.matches("/api/orders/456"));
        assertFalse(route.matches("/api/users"));
    }

    @Test
    void testMatches_SimpleWildcard() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/*/test");
        route.setEnabled(true);

        assertTrue(route.matches("/api/v1/test"));
        assertTrue(route.matches("/api/users/test"));
        assertFalse(route.matches("/api/v1/other"));
    }

    @Test
    void testMatches_ExactMatch() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/status");
        route.setEnabled(true);

        assertTrue(route.matches("/api/status"));
        assertTrue(route.matches("/api/status/health")); // prefix match
        assertFalse(route.matches("/api/status2"));
        assertFalse(route.matches("/api/other"));
    }

    @Test
    void testMatches_DisabledRoute() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/users/**");
        route.setEnabled(false);

        assertFalse(route.matches("/api/users/123"));
    }

    @Test
    void testMatches_NullPattern() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern(null);
        route.setEnabled(true);

        assertFalse(route.matches("/api/users/123"));
    }

    @Test
    void testBuildTargetUrl_WildcardPattern() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/users/**");
        route.setTargetUrl("http://user-service.com");

        assertEquals("http://user-service.com/123", route.buildTargetUrl("/api/users/123"));
        assertEquals("http://user-service.com/123/profile", route.buildTargetUrl("/api/users/123/profile"));
    }

    @Test
    void testBuildTargetUrl_WildcardPatternWithTrailingSlash() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/users/**");
        route.setTargetUrl("http://user-service.com/");

        assertEquals("http://user-service.com/123", route.buildTargetUrl("/api/users/123"));
        assertEquals("http://user-service.com/123/profile", route.buildTargetUrl("/api/users/123/profile"));
    }

    @Test
    void testBuildTargetUrl_SingleWildcard() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/users/*");
        route.setTargetUrl("http://user-service.com");

        assertEquals("http://user-service.com/123", route.buildTargetUrl("/api/users/123"));
    }

    @Test
    void testBuildTargetUrl_SimpleWildcard() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/*/test");
        route.setTargetUrl("http://service.com");

        assertEquals("http://service.com/api/v1/test", route.buildTargetUrl("/api/v1/test"));
    }

    @Test
    void testBuildTargetUrl_SimpleWildcardWithTrailingSlash() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/*/test");
        route.setTargetUrl("http://service.com/");

        assertEquals("http://service.com/api/v1/test", route.buildTargetUrl("/api/v1/test"));
    }

    @Test
    void testBuildTargetUrl_ExactMatch() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/status");
        route.setTargetUrl("http://status-service.com");

        assertEquals("http://status-service.com", route.buildTargetUrl("/api/status"));
        assertEquals("http://status-service.com/health", route.buildTargetUrl("/api/status/health"));
    }

    @Test
    void testBuildTargetUrl_ExactMatchWithTrailingSlash() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern("/api/status");
        route.setTargetUrl("http://status-service.com/");

        assertEquals("http://status-service.com/", route.buildTargetUrl("/api/status"));
        assertEquals("http://status-service.com/health", route.buildTargetUrl("/api/status/health"));
    }

    @Test
    void testBuildTargetUrl_NullPattern() {
        ProximaConfig.ConfigRoute route = new ProximaConfig.ConfigRoute();
        route.setPathPattern(null);
        route.setTargetUrl("http://service.com");

        assertEquals("http://service.com", route.buildTargetUrl("/api/test"));
    }
}