package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.oidc.OidcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of OIDC client service
 * In production, this would be backed by a database
 */
@Service
public class OidcClientServiceImpl implements OidcClientService {

    private static final Logger logger = LoggerFactory.getLogger(OidcClientServiceImpl.class);

    private final Map<String, OidcClient> clients = new ConcurrentHashMap<>();

    public OidcClientServiceImpl() {
        // Initialize with some default clients for testing
        initializeDefaultClients();
    }

    private void initializeDefaultClients() {
        // Self-test client for Proxima
        OidcClient selfTestClient = OidcClient.create(
            "proxima-self-test",
            "Proxima Self-Test",
            "Built-in client for testing OIDC functionality within Proxima itself",
            List.of("http://localhost:8080/proxima/ui/oidc-testing/callback"),
            List.of("openid", "profile", "email", "groups")
        );
        clients.put(selfTestClient.getClientId(), selfTestClient);

        // Test client
        OidcClient testClient = OidcClient.create(
            "test-client",
            "Test Application",
            "Default test client for OIDC testing",
            List.of("http://localhost:8080/callback", "http://localhost:3000/callback"),
            List.of("openid", "profile", "email")
        );
        clients.put(testClient.getClientId(), testClient);

        // Demo client
        OidcClient demoClient = OidcClient.create(
            "demo-app",
            "Demo Application",
            "Demo client for showcasing OIDC functionality",
            List.of("http://localhost:8080/auth/callback"),
            List.of("openid", "profile", "email", "groups")
        );
        clients.put(demoClient.getClientId(), demoClient);

        logger.info("Initialized {} default OIDC clients", clients.size());
    }

    @Override
    public List<OidcClient> getAllClients() {
        return List.copyOf(clients.values());
    }

    @Override
    public Optional<OidcClient> getClientById(String clientId) {
        return Optional.ofNullable(clients.get(clientId));
    }

    @Override
    public OidcClient registerClient(OidcClient client) {
        if (client == null || !client.isValid()) {
            throw new IllegalArgumentException("Invalid client configuration");
        }

        if (clients.containsKey(client.getClientId())) {
            throw new IllegalArgumentException("Client with ID '" + client.getClientId() + "' already exists");
        }

        client.setCreatedAt(Instant.now());
        client.setModifiedAt(Instant.now());
        clients.put(client.getClientId(), client);

        logger.info("Registered new OIDC client: {}", client.getClientId());
        return client;
    }

    @Override
    public OidcClient updateClient(String clientId, OidcClient client) {
        if (client == null || !client.isValid()) {
            throw new IllegalArgumentException("Invalid client configuration");
        }

        OidcClient existingClient = clients.get(clientId);
        if (existingClient == null) {
            throw new IllegalArgumentException("Client with ID '" + clientId + "' not found");
        }

        // Preserve creation timestamp
        client.setCreatedAt(existingClient.getCreatedAt());
        client.setModifiedAt(Instant.now());
        clients.put(clientId, client);

        logger.info("Updated OIDC client: {}", clientId);
        return client;
    }

    @Override
    public boolean deleteClient(String clientId) {
        if (clients.remove(clientId) != null) {
            logger.info("Deleted OIDC client: {}", clientId);
            return true;
        }
        return false;
    }

    @Override
    public boolean setClientEnabled(String clientId, boolean enabled) {
        OidcClient client = clients.get(clientId);
        if (client != null) {
            client.setEnabled(enabled);
            client.setModifiedAt(Instant.now());
            logger.info("Set OIDC client {} enabled status to: {}", clientId, enabled);
            return true;
        }
        return false;
    }

    @Override
    public boolean isValidRedirectUri(String clientId, String redirectUri) {
        OidcClient client = clients.get(clientId);
        if (client == null || !client.isEnabled()) {
            return false;
        }

        return client.getRedirectUris().contains(redirectUri);
    }

    @Override
    public ClientStats getStats() {
        int total = clients.size();
        int enabled = (int) clients.values().stream().filter(OidcClient::isEnabled).count();
        int disabled = total - enabled;

        return new ClientStats(total, enabled, disabled);
    }

    /**
     * Update the self-test client with dynamic redirect URI
     */
    public void updateSelfTestClientRedirectUri(String baseUrl) {
        OidcClient selfTestClient = clients.get("proxima-self-test");
        if (selfTestClient != null) {
            String dynamicRedirectUri = baseUrl + "/proxima/ui/oidc-testing/callback";

            // Update redirect URIs to include the dynamic one
            List<String> updatedRedirectUris = List.of(
                dynamicRedirectUri,
                "http://localhost:8080/proxima/ui/oidc-testing/callback" // Keep localhost as fallback
            );

            selfTestClient.setRedirectUris(updatedRedirectUris);
            selfTestClient.setModifiedAt(java.time.Instant.now());

            logger.info("Updated self-test client redirect URI to: {}", dynamicRedirectUri);
        }
    }
}