package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.oidc.AuthorizationCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthorizationCodeServiceImpl implements AuthorizationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationCodeServiceImpl.class);

    private final ConcurrentHashMap<String, AuthorizationCode> authorizationCodes = new ConcurrentHashMap<>();
    private final AtomicInteger totalGenerated = new AtomicInteger(0);

    @Override
    public AuthorizationCode generateAuthorizationCode(String clientId, String redirectUri, String scope,
                                                     String state, String nonce, String subject) {
        AuthorizationCode code = AuthorizationCode.create(clientId, redirectUri, scope, state, nonce, subject);
        authorizationCodes.put(code.getCode(), code);
        totalGenerated.incrementAndGet();

        logger.debug("Generated authorization code for client: {} with scope: {}", clientId, scope);
        return code;
    }

    @Override
    public AuthorizationCode validateAndConsumeCode(String code, String clientId, String redirectUri) {
        AuthorizationCode authCode = authorizationCodes.get(code);

        if (authCode == null) {
            logger.warn("Authorization code not found: {}", code);
            throw new IllegalArgumentException("Invalid authorization code");
        }

        if (!authCode.isValid()) {
            logger.warn("Authorization code expired or already used: {}", code);
            throw new IllegalArgumentException("Authorization code expired or already used");
        }

        if (!authCode.getClientId().equals(clientId)) {
            logger.warn("Client ID mismatch for authorization code: {} expected: {} actual: {}",
                       code, authCode.getClientId(), clientId);
            throw new IllegalArgumentException("Client ID mismatch");
        }

        if (!authCode.getRedirectUri().equals(redirectUri)) {
            logger.warn("Redirect URI mismatch for authorization code: {} expected: {} actual: {}",
                       code, authCode.getRedirectUri(), redirectUri);
            throw new IllegalArgumentException("Redirect URI mismatch");
        }

        // Mark as used and remove from storage
        authCode.markAsUsed();
        authorizationCodes.remove(code);

        logger.info("Authorization code consumed successfully for client: {}", clientId);
        return authCode;
    }

    @Override
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredCodes() {
        Instant now = Instant.now();
        AtomicInteger cleaned = new AtomicInteger(0);

        authorizationCodes.entrySet().removeIf(entry -> {
            if (entry.getValue().getExpiresAt().isBefore(now)) {
                cleaned.incrementAndGet();
                return true;
            }
            return false;
        });

        if (cleaned.get() > 0) {
            logger.info("Cleaned up {} expired authorization codes", cleaned.get());
        }
    }

    @Override
    public AuthorizationCodeStats getStats() {
        int active = 0;
        int expired = 0;
        Instant now = Instant.now();

        for (AuthorizationCode code : authorizationCodes.values()) {
            if (code.getExpiresAt().isBefore(now)) {
                expired++;
            } else {
                active++;
            }
        }

        return new AuthorizationCodeStats(active, expired, totalGenerated.get());
    }
}