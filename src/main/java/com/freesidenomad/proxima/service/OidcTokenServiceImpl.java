package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.HeaderPreset;
import com.freesidenomad.proxima.model.oidc.OidcPresetConfig;
import com.freesidenomad.proxima.model.oidc.OidcTokens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OidcTokenServiceImpl implements OidcTokenService {

    private final JwtService jwtService;
    private final ConfigurationService configurationService;
    private final String baseUrl;

    // Cache for tokens with automatic expiration
    private final Map<String, OidcTokens> tokenCache = new ConcurrentHashMap<>();

    public OidcTokenServiceImpl(
            JwtService jwtService,
            ConfigurationService configurationService,
            @Value("${proxima.base-url:http://localhost:8080}") String baseUrl) {
        this.jwtService = jwtService;
        this.configurationService = configurationService;
        this.baseUrl = baseUrl;
    }

    @Override
    public OidcTokens generateTokensForPreset(HeaderPreset preset) {
        if (!preset.isOidcEnabled()) {
            throw new IllegalArgumentException("Preset does not have OIDC enabled: " + preset.getName());
        }

        OidcPresetConfig oidcConfig = preset.getOidcConfig();

        // Create claims for ID token
        Map<String, Object> idTokenClaims = createIdTokenClaims(oidcConfig);

        // Create claims for access token
        Map<String, Object> accessTokenClaims = createAccessTokenClaims(oidcConfig);

        // Generate tokens using JwtService
        Duration expiration = Duration.ofSeconds(oidcConfig.getTokenExpirationSeconds());

        String idToken = jwtService.generateToken(
                oidcConfig.getSubject(),
                idTokenClaims,
                expiration,
                oidcConfig.getAlgorithm(),
                oidcConfig.getKeyId()
        );

        String accessToken = jwtService.generateToken(
                oidcConfig.getSubject(),
                accessTokenClaims,
                expiration,
                oidcConfig.getAlgorithm(),
                oidcConfig.getKeyId()
        );

        Instant now = Instant.now();
        OidcTokens tokens = OidcTokens.builder()
                .idToken(idToken)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(oidcConfig.getTokenExpirationSeconds())
                .expiresAt(now.plusSeconds(oidcConfig.getTokenExpirationSeconds()))
                .scope(String.join(" ", oidcConfig.getScopes()))
                .createdAt(now)
                .build();

        // Cache the tokens
        tokenCache.put(preset.getName(), tokens);
        preset.cacheTokens(tokens);

        log.info("Generated OIDC tokens for preset: {}", preset.getName());
        return tokens;
    }

    @Override
    public OidcTokens getValidTokensForPreset(String presetName) {
        // Check cache first
        OidcTokens cachedTokens = tokenCache.get(presetName);

        if (cachedTokens != null && !cachedTokens.isExpired()) {
            return cachedTokens;
        }

        // Tokens expired or not found, regenerate
        HeaderPreset preset = configurationService.getPreset(presetName);
        if (preset == null || !preset.isOidcEnabled()) {
            return null;
        }

        return generateTokensForPreset(preset);
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void generateTokensForAllPresets() {
        log.info("Generating OIDC tokens for all enabled presets on startup");

        List<HeaderPreset> oidcPresets = configurationService.getOidcEnabledPresets();

        for (HeaderPreset preset : oidcPresets) {
            try {
                generateTokensForPreset(preset);
            } catch (Exception e) {
                log.error("Failed to generate tokens for preset: {}", preset.getName(), e);
            }
        }

        log.info("Completed startup token generation for {} presets", oidcPresets.size());
    }

    @Override
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void refreshExpiringTokens() {
        log.debug("Checking for expiring tokens to refresh");

        long refreshThreshold = 300; // Refresh if expires within 5 minutes

        for (Map.Entry<String, OidcTokens> entry : tokenCache.entrySet()) {
            OidcTokens tokens = entry.getValue();

            // Check if token expires soon
            long secondsUntilExpiry = Duration.between(
                    Instant.now(),
                    tokens.getExpiresAt()
            ).getSeconds();

            if (secondsUntilExpiry <= refreshThreshold) {
                String presetName = entry.getKey();
                log.info("Refreshing expiring tokens for preset: {}", presetName);

                try {
                    HeaderPreset preset = configurationService.getPreset(presetName);
                    if (preset != null && preset.isOidcEnabled()) {
                        generateTokensForPreset(preset);
                    }
                } catch (Exception e) {
                    log.error("Failed to refresh tokens for preset: {}", presetName, e);
                }
            }
        }
    }

    @Override
    public void clearTokensForPreset(String presetName) {
        tokenCache.remove(presetName);
        log.info("Cleared cached tokens for preset: {}", presetName);
    }

    @Override
    public Map<String, Object> getTokenCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedPresets", tokenCache.size());
        stats.put("presets", new ArrayList<>(tokenCache.keySet()));

        long expiredCount = tokenCache.values().stream()
                .mapToLong(tokens -> tokens.isExpired() ? 1 : 0)
                .sum();

        stats.put("expiredTokens", expiredCount);
        stats.put("validTokens", tokenCache.size() - expiredCount);

        return stats;
    }

    private Map<String, Object> createIdTokenClaims(OidcPresetConfig config) {
        List<String> audience = Arrays.asList(config.getClientId() != null ? config.getClientId() : "proxima");
        return config.toClaims(baseUrl, audience);
    }

    private Map<String, Object> createAccessTokenClaims(OidcPresetConfig config) {
        List<String> audience = Arrays.asList("proxima-api");

        Map<String, Object> claims = config.toClaims(baseUrl, audience);
        claims.put("scope", String.join(" ", config.getScopes()));
        claims.put("token_type", "access_token");

        return claims;
    }
}