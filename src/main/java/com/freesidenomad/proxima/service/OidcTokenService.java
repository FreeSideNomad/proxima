package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.oidc.OidcTokens;

import java.util.Map;

/**
 * Service for generating, caching, and managing OIDC tokens for configured presets.
 * Integrates with existing JwtService to generate tokens and provides automatic
 * token refresh and caching capabilities.
 */
public interface OidcTokenService {

    /**
     * Generate OIDC tokens (ID and access tokens) for a specific preset.
     *
     * @param preset HeaderPreset with OIDC configuration
     * @return Generated OidcTokens containing both ID and access tokens
     * @throws IllegalArgumentException if preset does not have OIDC enabled
     */
    OidcTokens generateTokensForPreset(HeaderPreset preset);

    /**
     * Get valid cached tokens for preset, regenerating if expired or not found.
     *
     * @param presetName Name of the preset to get tokens for
     * @return Valid OidcTokens or null if preset not found or not OIDC-enabled
     */
    OidcTokens getValidTokensForPreset(String presetName);

    /**
     * Generate tokens for all OIDC-enabled presets.
     * Called automatically on application startup.
     */
    void generateTokensForAllPresets();

    /**
     * Refresh tokens that are close to expiration.
     * Called automatically by scheduled task.
     */
    void refreshExpiringTokens();

    /**
     * Clear cached tokens for a specific preset.
     *
     * @param presetName Name of preset to clear tokens for
     */
    void clearTokensForPreset(String presetName);

    /**
     * Get statistics about the token cache.
     *
     * @return Map containing cache statistics
     */
    Map<String, Object> getTokenCacheStats();
}