package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.RouteRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private ProximaProperties proximaProperties;

    @InjectMocks
    private RouteService routeService;

    private ProximaProperties.Downstream downstream;
    private List<RouteRule> routes;

    @BeforeEach
    void setUp() {
        downstream = new ProximaProperties.Downstream();
        downstream.setUrl("http://default-service.com");

        RouteRule userRoute = new RouteRule();
        userRoute.setPathPattern("/api/users/**");
        userRoute.setTargetUrl("http://user-service.com");
        userRoute.setDescription("User service");
        userRoute.setEnabled(true);

        RouteRule invoiceRoute = new RouteRule();
        invoiceRoute.setPathPattern("/api/invoices/**");
        invoiceRoute.setTargetUrl("http://invoice-service.com");
        invoiceRoute.setDescription("Invoice service");
        invoiceRoute.setEnabled(true);

        RouteRule disabledRoute = new RouteRule();
        disabledRoute.setPathPattern("/api/disabled/**");
        disabledRoute.setTargetUrl("http://disabled-service.com");
        disabledRoute.setDescription("Disabled service");
        disabledRoute.setEnabled(false);

        routes = List.of(userRoute, invoiceRoute, disabledRoute);
    }

    @Test
    void testResolveTargetUrlWithMatchingRoute() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        String result = routeService.resolveTargetUrl("/api/users/123");

        assertEquals("http://user-service.com/api/users/123", result);
    }

    @Test
    void testResolveTargetUrlWithFallback() {
        when(proximaProperties.getRoutes()).thenReturn(routes);
        when(proximaProperties.getDownstream()).thenReturn(downstream);

        String result = routeService.resolveTargetUrl("/api/unknown/endpoint");

        assertEquals("http://default-service.com/api/unknown/endpoint", result);
    }

    @Test
    void testResolveTargetUrlDisabledRoute() {
        when(proximaProperties.getRoutes()).thenReturn(routes);
        when(proximaProperties.getDownstream()).thenReturn(downstream);

        String result = routeService.resolveTargetUrl("/api/disabled/test");

        assertEquals("http://default-service.com/api/disabled/test", result);
    }

    @Test
    void testFindMatchingRoute() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        Optional<RouteRule> result = routeService.findMatchingRoute("/api/users/123");

        assertTrue(result.isPresent());
        assertEquals("/api/users/**", result.get().getPathPattern());
    }

    @Test
    void testFindMatchingRouteNotFound() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        Optional<RouteRule> result = routeService.findMatchingRoute("/api/unknown");

        assertFalse(result.isPresent());
    }

    @Test
    void testHasRoutes() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        assertTrue(routeService.hasRoutes());
    }

    @Test
    void testHasRoutesEmpty() {
        when(proximaProperties.getRoutes()).thenReturn(List.of());

        assertFalse(routeService.hasRoutes());
    }

    @Test
    void testGetRouteCount() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        assertEquals(3, routeService.getRouteCount());
    }

    @Test
    void testGetEnabledRouteCount() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        assertEquals(2, routeService.getEnabledRouteCount());
    }

    @Test
    void testGetAllRoutes() {
        when(proximaProperties.getRoutes()).thenReturn(routes);

        List<RouteRule> result = routeService.getAllRoutes();

        assertEquals(routes, result);
    }
}