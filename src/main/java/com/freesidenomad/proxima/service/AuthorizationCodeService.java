package com.freesidenomad.proxima.service;

import com.freesidenomad.proxima.model.oidc.AuthorizationCode;

public interface AuthorizationCodeService {

    /**
     * Generate and store a new authorization code
     */
    AuthorizationCode generateAuthorizationCode(String clientId, String redirectUri, String scope,
                                              String state, String nonce, String subject);

    /**
     * Validate and consume an authorization code
     */
    AuthorizationCode validateAndConsumeCode(String code, String clientId, String redirectUri);

    /**
     * Clean up expired authorization codes
     */
    void cleanupExpiredCodes();

    /**
     * Get statistics about authorization codes
     */
    AuthorizationCodeStats getStats();

    class AuthorizationCodeStats {
        public final int activeCodes;
        public final int expiredCodes;
        public final int totalGenerated;

        public AuthorizationCodeStats(int activeCodes, int expiredCodes, int totalGenerated) {
            this.activeCodes = activeCodes;
            this.expiredCodes = expiredCodes;
            this.totalGenerated = totalGenerated;
        }
    }
}