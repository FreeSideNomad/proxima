package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.oidc.OidcClient;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing OIDC clients
 */
public interface OidcClientService {

    /**
     * Get all registered OIDC clients
     */
    List<OidcClient> getAllClients();

    /**
     * Get an OIDC client by client ID
     */
    Optional<OidcClient> getClientById(String clientId);

    /**
     * Register a new OIDC client
     */
    OidcClient registerClient(OidcClient client);

    /**
     * Update an existing OIDC client
     */
    OidcClient updateClient(String clientId, OidcClient client);

    /**
     * Delete an OIDC client
     */
    boolean deleteClient(String clientId);

    /**
     * Enable/disable an OIDC client
     */
    boolean setClientEnabled(String clientId, boolean enabled);

    /**
     * Validate if a client is authorized for the given redirect URI
     */
    boolean isValidRedirectUri(String clientId, String redirectUri);

    /**
     * Get client statistics
     */
    ClientStats getStats();

    /**
     * Statistics about OIDC clients
     */
    record ClientStats(int totalClients, int enabledClients, int disabledClients) {}
}