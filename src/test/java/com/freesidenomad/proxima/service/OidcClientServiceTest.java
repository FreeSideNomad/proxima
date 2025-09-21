package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.oidc.OidcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OidcClientServiceTest {

    private OidcClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OidcClientServiceImpl();
    }

    @Test
    void shouldInitializeWithDefaultClients() {
        List<OidcClient> clients = service.getAllClients();
        assertEquals(3, clients.size());

        Optional<OidcClient> testClient = service.getClientById("test-client");
        assertTrue(testClient.isPresent());
        assertEquals("Test Application", testClient.get().getClientName());
        assertTrue(testClient.get().isEnabled());
    }

    @Test
    void shouldRegisterNewClient() {
        OidcClient newClient = OidcClient.create(
            "new-client",
            "New Application",
            "A new test client",
            List.of("http://localhost:9000/callback"),
            List.of("openid", "profile")
        );

        OidcClient registered = service.registerClient(newClient);
        assertNotNull(registered);
        assertEquals("new-client", registered.getClientId());
        assertEquals("New Application", registered.getClientName());
        assertTrue(registered.isEnabled());

        // Verify it can be retrieved
        Optional<OidcClient> retrieved = service.getClientById("new-client");
        assertTrue(retrieved.isPresent());
        assertEquals("New Application", retrieved.get().getClientName());
    }

    @Test
    void shouldThrowExceptionForDuplicateClientId() {
        OidcClient duplicateClient = OidcClient.create(
            "test-client",  // This already exists
            "Duplicate Application",
            "Should fail",
            List.of("http://localhost:9000/callback"),
            List.of("openid")
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.registerClient(duplicateClient)
        );

        assertEquals("Client with ID 'test-client' already exists", exception.getMessage());
    }

    @Test
    void shouldUpdateExistingClient() {
        OidcClient updatedClient = OidcClient.create(
            "test-client",
            "Updated Test Application",
            "Updated description",
            List.of("http://localhost:8080/callback", "http://localhost:4000/callback"),
            List.of("openid", "profile", "email", "groups")
        );

        OidcClient result = service.updateClient("test-client", updatedClient);
        assertNotNull(result);
        assertEquals("Updated Test Application", result.getClientName());
        assertEquals("Updated description", result.getDescription());
        assertEquals(4, result.getAllowedScopes().size());
    }

    @Test
    void shouldThrowExceptionForUpdatingNonExistentClient() {
        OidcClient client = OidcClient.create(
            "non-existent",
            "Non-existent Application",
            "Should fail",
            List.of("http://localhost:9000/callback"),
            List.of("openid")
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateClient("non-existent", client)
        );

        assertEquals("Client with ID 'non-existent' not found", exception.getMessage());
    }

    @Test
    void shouldDeleteClient() {
        assertTrue(service.deleteClient("test-client"));
        assertFalse(service.getClientById("test-client").isPresent());

        // Deleting non-existent client should return false
        assertFalse(service.deleteClient("non-existent"));
    }

    @Test
    void shouldSetClientEnabledStatus() {
        // Initially enabled
        Optional<OidcClient> client = service.getClientById("test-client");
        assertTrue(client.isPresent());
        assertTrue(client.get().isEnabled());

        // Disable
        assertTrue(service.setClientEnabled("test-client", false));
        client = service.getClientById("test-client");
        assertTrue(client.isPresent());
        assertFalse(client.get().isEnabled());

        // Enable again
        assertTrue(service.setClientEnabled("test-client", true));
        client = service.getClientById("test-client");
        assertTrue(client.isPresent());
        assertTrue(client.get().isEnabled());

        // Non-existent client should return false
        assertFalse(service.setClientEnabled("non-existent", true));
    }

    @Test
    void shouldValidateRedirectUri() {
        // Valid redirect URI for test-client
        assertTrue(service.isValidRedirectUri("test-client", "http://localhost:8080/callback"));
        assertTrue(service.isValidRedirectUri("test-client", "http://localhost:3000/callback"));

        // Invalid redirect URI
        assertFalse(service.isValidRedirectUri("test-client", "http://evil.com/callback"));

        // Non-existent client
        assertFalse(service.isValidRedirectUri("non-existent", "http://localhost:8080/callback"));

        // Disabled client
        service.setClientEnabled("test-client", false);
        assertFalse(service.isValidRedirectUri("test-client", "http://localhost:8080/callback"));
    }

    @Test
    void shouldGetStats() {
        OidcClientService.ClientStats stats = service.getStats();
        assertEquals(3, stats.totalClients());
        assertEquals(3, stats.enabledClients());
        assertEquals(0, stats.disabledClients());

        // Disable one client
        service.setClientEnabled("test-client", false);
        stats = service.getStats();
        assertEquals(3, stats.totalClients());
        assertEquals(2, stats.enabledClients());
        assertEquals(1, stats.disabledClients());

        // Add a new client
        OidcClient newClient = OidcClient.create(
            "new-client",
            "New Application",
            "Test client",
            List.of("http://localhost:9000/callback"),
            List.of("openid")
        );
        service.registerClient(newClient);

        stats = service.getStats();
        assertEquals(4, stats.totalClients());
        assertEquals(3, stats.enabledClients());
        assertEquals(1, stats.disabledClients());
    }

    @Test
    void shouldRejectInvalidClients() {
        // Null client
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.registerClient(null)
        );
        assertEquals("Invalid client configuration", exception.getMessage());

        // Client with empty client ID
        OidcClient invalidClient = OidcClient.builder()
            .clientId("")
            .clientName("Invalid Client")
            .redirectUris(List.of("http://localhost:8080/callback"))
            .allowedScopes(List.of("openid"))
            .enabled(true)
            .build();

        exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.registerClient(invalidClient)
        );
        assertEquals("Invalid client configuration", exception.getMessage());
    }
}