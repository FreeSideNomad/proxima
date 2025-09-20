package com.freesidenomad.proxima.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final Map<String, SecretKey> hmacKeys = new ConcurrentHashMap<>();
    private final Map<String, KeyPair> rsaKeys = new ConcurrentHashMap<>();
    private SecretKey defaultHmacKey;
    private KeyPair defaultRsaKey;

    public JwtService() {
        try {
            initializeDefaultKeys();
        } catch (Exception e) {
            // Re-throw as unchecked to fail fast during Spring initialization
            throw new IllegalStateException("Failed to initialize JWT service", e);
        }
    }

    private void initializeDefaultKeys() throws NoSuchAlgorithmException {
        // Generate default HMAC key (HS256)
        defaultHmacKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        hmacKeys.put("default", defaultHmacKey);

        // Generate default RSA key pair (RS256)
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        defaultRsaKey = keyPairGenerator.generateKeyPair();
        rsaKeys.put("default", defaultRsaKey);

        logger.info("JWT Service initialized with default HMAC and RSA keys");
    }

    public String generateToken(String subject, Map<String, Object> claims, Duration expiration, String algorithm) {
        return generateToken(subject, claims, expiration, algorithm, "default");
    }

    public String generateToken(String subject, Map<String, Object> claims, Duration expiration, String algorithm, String keyId) {
        Instant now = Instant.now();
        Date expirationDate = Date.from(now.plus(expiration));

        var builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(expirationDate)
                .claims(claims);

        switch (algorithm.toUpperCase(Locale.ENGLISH)) {
            case "HS256":
                SecretKey hmacKey = hmacKeys.get(keyId);
                if (hmacKey == null) {
                    throw new IllegalArgumentException("HMAC key not found: " + keyId);
                }
                return builder.signWith(hmacKey, SignatureAlgorithm.HS256).compact();

            case "RS256":
                KeyPair rsaKey = rsaKeys.get(keyId);
                if (rsaKey == null) {
                    throw new IllegalArgumentException("RSA key not found: " + keyId);
                }
                return builder
                        .setHeaderParam("kid", keyId)
                        .signWith(rsaKey.getPrivate(), SignatureAlgorithm.RS256)
                        .compact();

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    public Map<String, Object> generateTokenResponse(String subject, Map<String, Object> claims, Duration expiration, String algorithm, String keyId) {
        String token = generateToken(subject, claims, expiration, algorithm, keyId);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("subject", subject);
        response.put("algorithm", algorithm);
        response.put("keyId", keyId);
        response.put("expiresIn", expiration.getSeconds());
        response.put("expiresAt", Instant.now().plus(expiration).toString());
        response.put("claims", claims);

        return response;
    }

    public String generateHmacKey(String keyId) {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        hmacKeys.put(keyId, key);

        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        logger.info("Generated new HMAC key with ID: {}", keyId);

        return encodedKey;
    }

    public Map<String, String> generateRsaKeyPair(String keyId) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            rsaKeys.put(keyId, keyPair);

            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            Map<String, String> keys = new HashMap<>();
            keys.put("keyId", keyId);
            keys.put("publicKey", publicKey);
            keys.put("privateKey", privateKey);
            keys.put("algorithm", "RS256");

            logger.info("Generated new RSA key pair with ID: {}", keyId);

            return keys;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public Map<String, Object> getKeyInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("hmacKeys", hmacKeys.keySet());
        info.put("rsaKeys", rsaKeys.keySet());
        info.put("totalKeys", hmacKeys.size() + rsaKeys.size());

        return info;
    }

    public void deleteKey(String keyId) {
        boolean removed = false;

        if (hmacKeys.remove(keyId) != null) {
            removed = true;
            logger.info("Deleted HMAC key: {}", keyId);
        }

        if (rsaKeys.remove(keyId) != null) {
            removed = true;
            logger.info("Deleted RSA key: {}", keyId);
        }

        if (!removed) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }

        if ("default".equals(keyId)) {
            try {
                initializeDefaultKeys();
                logger.info("Regenerated default keys after deletion");
            } catch (NoSuchAlgorithmException e) {
                logger.error("Failed to regenerate default keys", e);
                throw new RuntimeException("Failed to regenerate default keys", e);
            }
        }
    }

    public boolean keyExists(String keyId) {
        return hmacKeys.containsKey(keyId) || rsaKeys.containsKey(keyId);
    }

    public String getPublicKey(String keyId) {
        KeyPair keyPair = rsaKeys.get(keyId);
        if (keyPair == null) {
            throw new IllegalArgumentException("RSA key not found: " + keyId);
        }

        // Convert to PEM format for jwt.io compatibility
        byte[] encoded = keyPair.getPublic().getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);

        // Format as PEM with proper line breaks
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");

        // Add line breaks every 64 characters
        for (int i = 0; i < base64.length(); i += 64) {
            int endIndex = Math.min(i + 64, base64.length());
            pem.append(base64.substring(i, endIndex)).append("\n");
        }

        pem.append("-----END PUBLIC KEY-----");
        return pem.toString();
    }

    public Map<String, Object> getJwks() {
        Map<String, Object> jwks = new HashMap<>();
        Map<String, Object>[] keys = rsaKeys.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> jwk = new HashMap<>();
                    jwk.put("kty", "RSA");
                    jwk.put("use", "sig");
                    jwk.put("kid", entry.getKey());
                    jwk.put("alg", "RS256");

                    // Extract RSA public key components for OAuth compliance
                    RSAPublicKey publicKey = (RSAPublicKey) entry.getValue().getPublic();

                    // Convert modulus (n) to base64url without padding
                    BigInteger modulus = publicKey.getModulus();
                    byte[] modulusBytes = modulus.toByteArray();
                    // Remove leading zero byte if present (two's complement representation)
                    if (modulusBytes[0] == 0 && modulusBytes.length > 1) {
                        byte[] temp = new byte[modulusBytes.length - 1];
                        System.arraycopy(modulusBytes, 1, temp, 0, temp.length);
                        modulusBytes = temp;
                    }
                    jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(modulusBytes));

                    // Convert exponent (e) to base64url without padding
                    BigInteger exponent = publicKey.getPublicExponent();
                    byte[] exponentBytes = exponent.toByteArray();
                    jwk.put("e", Base64.getUrlEncoder().withoutPadding().encodeToString(exponentBytes));

                    return jwk;
                })
                .toArray(Map[]::new);

        jwks.put("keys", keys);
        return jwks;
    }
}