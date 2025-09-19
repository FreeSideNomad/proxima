package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.config.ProximaProperties;
import com.freesidenomad.proxima.model.ProximaConfig;
import com.freesidenomad.proxima.model.RouteRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private ProximaProperties proximaProperties;

    @Mock
    private JsonConfigurationService jsonConfigurationService;

    @InjectMocks
    private RouteService routeService;

    private ProximaConfig mockConfig;
    private List<RouteRule> routes;

    @BeforeEach
    void setUp() {
        mockConfig = new ProximaConfig();
        mockConfig.getDownstream().setUrl("http://default-service.com");

        List<ProximaConfig.ConfigRoute> configRoutes = new ArrayList<>();

        ProximaConfig.ConfigRoute userRoute = new ProximaConfig.ConfigRoute();
        userRoute.setPathPattern("/api/users/**");
        userRoute.setTargetUrl("http://user-service.com");
        userRoute.setDescription("User service");
        userRoute.setEnabled(true);
        configRoutes.add(userRoute);

        ProximaConfig.ConfigRoute invoiceRoute = new ProximaConfig.ConfigRoute();
        invoiceRoute.setPathPattern("/api/invoices/**");
        invoiceRoute.setTargetUrl("http://invoice-service.com");
        invoiceRoute.setDescription("Invoice service");
        invoiceRoute.setEnabled(true);
        configRoutes.add(invoiceRoute);

        ProximaConfig.ConfigRoute disabledRoute = new ProximaConfig.ConfigRoute();
        disabledRoute.setPathPattern("/api/disabled/**");
        disabledRoute.setTargetUrl("http://disabled-service.com");
        disabledRoute.setDescription("Disabled service");
        disabledRoute.setEnabled(false);
        configRoutes.add(disabledRoute);

        mockConfig.setRoutes(configRoutes);
        mockConfig.setReservedRoutes(Arrays.asList("/api/**", "/actuator/**", "/dashboard/**"));
    }

    @Test
    void testResolveTargetUrlWithMatchingRoute() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        String result = routeService.resolveTargetUrl("/api/users/123");

        assertEquals("http://user-service.com/123", result);
    }

    @Test
    void testResolveTargetUrlWithFallback() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        String result = routeService.resolveTargetUrl("/unknown/endpoint");

        assertEquals("http://default-service.com/unknown/endpoint", result);
    }

    @Test
    void testResolveTargetUrlDisabledRoute() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        String result = routeService.resolveTargetUrl("/api/disabled/test");

        assertEquals("http://default-service.com/api/disabled/test", result);
    }

    @Test
    void testFindMatchingRoute() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        Optional<RouteRule> result = routeService.findMatchingRoute("/api/users/123");

        assertTrue(result.isPresent());
        assertEquals("/api/users/**", result.get().getPathPattern());
    }

    @Test
    void testFindMatchingRouteNotFound() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        Optional<RouteRule> result = routeService.findMatchingRoute("/unknown");

        assertFalse(result.isPresent());
    }

    @Test
    void testHasRoutes() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        assertTrue(routeService.hasRoutes());
    }

    @Test
    void testHasRoutesEmpty() {
        mockConfig.setRoutes(new ArrayList<>());
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        assertFalse(routeService.hasRoutes());
    }

    @Test
    void testGetRouteCount() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        assertEquals(3, routeService.getRouteCount());
    }

    @Test
    void testGetEnabledRouteCount() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        assertEquals(2, routeService.getEnabledRouteCount());
    }

    @Test
    void testGetAllRoutes() {
        when(jsonConfigurationService.loadConfiguration()).thenReturn(mockConfig);

        List<RouteRule> result = routeService.getAllRoutes();

        assertEquals(3, result.size());
        assertEquals("/api/users/**", result.get(0).getPathPattern());
        assertEquals("/api/invoices/**", result.get(1).getPathPattern());
        assertEquals("/api/disabled/**", result.get(2).getPathPattern());
    }
}