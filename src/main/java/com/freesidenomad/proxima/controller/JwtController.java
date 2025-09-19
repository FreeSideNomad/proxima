package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "JWT", description = "JWT token generation and cryptographic key management")
public class JwtController {

    private static final Logger logger = LoggerFactory.getLogger(JwtController.class);

    @Autowired
    private JwtService jwtService;

    @PostMapping("/tokens")
    @Operation(
        summary = "Generate JWT Token",
        description = "Generate a JWT token with custom claims, expiration, and signing algorithm",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Token generation request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TokenRequest.class),
                examples = @ExampleObject(
                    name = "Standard Token",
                    value = """
                    {
                      "subject": "user@example.com",
                      "algorithm": "HS256",
                      "keyId": "default",
                      "expirationSeconds": 3600,
                      "claims": {
                        "role": "admin",
                        "department": "IT",
                        "permissions": ["read", "write"]
                      }
                    }
                    """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token generated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Generated Token",
                    value = """
                    {
                      "token": "eyJhbGciOiJIUzI1NiJ9...",
                      "subject": "user@example.com",
                      "algorithm": "HS256",
                      "keyId": "default",
                      "expiresIn": 3600,
                      "expiresAt": "2023-12-01T12:00:00Z",
                      "claims": {
                        "role": "admin",
                        "department": "IT"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Error Response",
                    value = """
                    {
                      "status": "error",
                      "message": "Subject is required"
                    }
                    """
                )
            )
        )
    })
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
    @Schema(description = "JWT token generation request")
    public static class TokenRequest {
        @Schema(description = "JWT subject (sub claim)", example = "user@example.com", required = true)
        private String subject;

        @Schema(description = "Custom claims to include in the token", example = "{\"role\": \"admin\", \"department\": \"IT\"}")
        private Map<String, Object> claims;

        @Schema(description = "Token expiration time in seconds", example = "3600", defaultValue = "3600")
        private Long expirationSeconds;

        @Schema(description = "Signing algorithm", example = "HS256", allowableValues = {"HS256", "RS256"}, defaultValue = "HS256")
        private String algorithm;

        @Schema(description = "Key ID for signing", example = "default", defaultValue = "default")
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

    @Schema(description = "Cryptographic key generation request")
    public static class KeyRequest {
        @Schema(description = "Unique identifier for the key", example = "my-custom-key", required = true)
        private String keyId;

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
    }
}