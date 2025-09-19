package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/proxima/api/jwt")
public class JwtController {

    private static final Logger logger = LoggerFactory.getLogger(JwtController.class);

    @Autowired
    private JwtService jwtService;

    @PostMapping("/tokens")
    public ResponseEntity<Map<String, Object>> generateToken(@RequestBody TokenRequest request) {
        try {
            // Validate request
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Subject is required"));
            }

            String algorithm = request.getAlgorithm() != null ? request.getAlgorithm() : "HS256";
            String keyId = request.getKeyId() != null ? request.getKeyId() : "default";
            Duration expiration = request.getExpirationSeconds() != null ?
                Duration.ofSeconds(request.getExpirationSeconds()) : Duration.ofHours(1);

            Map<String, Object> claims = request.getClaims() != null ? request.getClaims() : new HashMap<>();

            Map<String, Object> response = jwtService.generateTokenResponse(
                request.getSubject(),
                claims,
                expiration,
                algorithm,
                keyId
            );

            logger.info("Generated JWT token for subject: {} with algorithm: {}", request.getSubject(), algorithm);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating JWT token", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/keys/hmac")
    public ResponseEntity<Map<String, Object>> generateHmacKey(@RequestBody KeyRequest request) {
        try {
            if (request.getKeyId() == null || request.getKeyId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Key ID is required"));
            }

            if (jwtService.keyExists(request.getKeyId())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Key ID already exists"));
            }

            String encodedKey = jwtService.generateHmacKey(request.getKeyId());

            Map<String, Object> response = new HashMap<>();
            response.put("keyId", request.getKeyId());
            response.put("algorithm", "HS256");
            response.put("key", encodedKey);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating HMAC key", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/keys/rsa")
    public ResponseEntity<Map<String, Object>> generateRsaKeyPair(@RequestBody KeyRequest request) {
        try {
            if (request.getKeyId() == null || request.getKeyId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Key ID is required"));
            }

            if (jwtService.keyExists(request.getKeyId())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Key ID already exists"));
            }

            Map<String, String> keyPair = jwtService.generateRsaKeyPair(request.getKeyId());

            Map<String, Object> response = new HashMap<>(keyPair);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating RSA key pair", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getKeys() {
        try {
            Map<String, Object> keyInfo = jwtService.getKeyInfo();
            keyInfo.put("status", "success");

            return ResponseEntity.ok(keyInfo);

        } catch (Exception e) {
            logger.error("Error retrieving key information", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/keys/{keyId}/public")
    public ResponseEntity<Map<String, Object>> getPublicKey(@PathVariable String keyId) {
        try {
            String publicKey = jwtService.getPublicKey(keyId);

            Map<String, Object> response = new HashMap<>();
            response.put("keyId", keyId);
            response.put("publicKey", publicKey);
            response.put("algorithm", "RS256");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving public key", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/keys/{keyId}")
    public ResponseEntity<Map<String, Object>> deleteKey(@PathVariable String keyId) {
        try {
            jwtService.deleteKey(keyId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Key deleted successfully");
            response.put("keyId", keyId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error deleting key", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {
        try {
            Map<String, Object> jwks = jwtService.getJwks();
            return ResponseEntity.ok(jwks);

        } catch (Exception e) {
            logger.error("Error retrieving JWKS", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", message);
        return error;
    }

    // Request DTOs
    public static class TokenRequest {
        private String subject;
        private Map<String, Object> claims;
        private Long expirationSeconds;
        private String algorithm;
        private String keyId;

        // Getters and setters
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public Map<String, Object> getClaims() { return claims; }
        public void setClaims(Map<String, Object> claims) { this.claims = claims; }

        public Long getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(Long expirationSeconds) { this.expirationSeconds = expirationSeconds; }

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
    }

    public static class KeyRequest {
        private String keyId;

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
    }
}