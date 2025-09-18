package com.freesidenomad.proxima.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteRuleTest {

    private RouteRule routeRule;

    @BeforeEach
    void setUp() {
        routeRule = new RouteRule();
        routeRule.setPathPattern("/api/users/**");
        routeRule.setTargetUrl("http://user-service.com");
        routeRule.setDescription("User service");
        routeRule.setEnabled(true);
    }

    @Test
    void testWildcardMatching() {
        assertTrue(routeRule.matches("/api/users/123"));
        assertTrue(routeRule.matches("/api/users/profile"));
        assertTrue(routeRule.matches("/api/users"));
        assertFalse(routeRule.matches("/api/accounts/123"));
    }

    @Test
    void testExactMatching() {
        routeRule.setPathPattern("/api/users");

        assertTrue(routeRule.matches("/api/users"));
        assertFalse(routeRule.matches("/api/users/123"));
        assertFalse(routeRule.matches("/api/accounts"));
    }

    @Test
    void testDisabledRoute() {
        routeRule.setEnabled(false);

        assertFalse(routeRule.matches("/api/users/123"));
    }

    @Test
    void testBuildTargetUrlWithWildcard() {
        String result = routeRule.buildTargetUrl("/api/users/123/profile");

        assertEquals("http://user-service.com/api/users/123/profile", result);
    }

    @Test
    void testBuildTargetUrlExact() {
        routeRule.setPathPattern("/api/users");

        String result = routeRule.buildTargetUrl("/api/users");

        assertEquals("http://user-service.com", result);
    }

    @Test
    void testNullPathPattern() {
        routeRule.setPathPattern(null);

        assertFalse(routeRule.matches("/api/users"));
    }

    @Test
    void testConstructorWithParameters() {
        RouteRule rule = new RouteRule("/test/**", "http://test.com", "Test service");

        assertEquals("/test/**", rule.getPathPattern());
        assertEquals("http://test.com", rule.getTargetUrl());
        assertEquals("Test service", rule.getDescription());
        assertTrue(rule.isEnabled());
    }
}