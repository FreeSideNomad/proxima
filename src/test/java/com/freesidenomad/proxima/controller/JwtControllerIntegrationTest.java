package com.freesidenomad.proxima.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

    @Test
    void shouldGenerateJwtToken() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("subject", "test@example.com");
        request.put("algorithm", "HS256");
        request.put("keyId", "default");
        request.put("expirationSeconds", 3600);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "admin");
        claims.put("department", "IT");
        request.put("claims", claims);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            createURLWithPort("/proxima/api/jwt/tokens"),
            entity,
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertTrue(body.containsKey("token"));
        assertEquals("test@example.com", body.get("subject"));
        assertEquals("HS256", body.get("algorithm"));
        assertEquals("default", body.get("keyId"));
        assertEquals(3600, body.get("expiresIn"));

        @SuppressWarnings("unchecked")
        Map<String, Object> returnedClaims = (Map<String, Object>) body.get("claims");
        assertEquals("admin", returnedClaims.get("role"));
        assertEquals("IT", returnedClaims.get("department"));
    }

    @Test
    void shouldGenerateRsaToken() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("subject", "api-client");
        request.put("algorithm", "RS256");
        request.put("keyId", "default");
        request.put("expirationSeconds", 1800);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            createURLWithPort("/proxima/api/jwt/tokens"),
            entity,
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertTrue(body.containsKey("token"));
        assertEquals("api-client", body.get("subject"));
        assertEquals("RS256", body.get("algorithm"));
        assertEquals(1800, body.get("expiresIn"));
    }

    @Test
    void shouldReturnErrorForMissingSubject() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("algorithm", "HS256");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            createURLWithPort("/proxima/api/jwt/tokens"),
            entity,
            Map.class
        );

        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("error", body.get("status"));
        assertEquals("Subject is required", body.get("message"));
    }

    @Test
    void shouldGenerateHmacKey() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("keyId", "test-hmac-key-integration");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            createURLWithPort("/proxima/api/jwt/keys/hmac"),
            entity,
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("success", body.get("status"));
        assertEquals("test-hmac-key-integration", body.get("keyId"));
        assertEquals("HS256", body.get("algorithm"));
        assertTrue(body.containsKey("key"));
    }

    @Test
    void shouldGenerateRsaKeyPair() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("keyId", "test-rsa-key-integration");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            createURLWithPort("/proxima/api/jwt/keys/rsa"),
            entity,
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("success", body.get("status"));
        assertEquals("test-rsa-key-integration", body.get("keyId"));
        assertEquals("RS256", body.get("algorithm"));
        assertTrue(body.containsKey("publicKey"));
        assertTrue(body.containsKey("privateKey"));
    }

    @Test
    void shouldGetKeyInformation() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            createURLWithPort("/proxima/api/jwt/keys"),
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("success", body.get("status"));
        assertTrue(body.containsKey("hmacKeys"));
        assertTrue(body.containsKey("rsaKeys"));
        assertTrue(body.containsKey("totalKeys"));
    }

    @Test
    void shouldReturnJwks() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            createURLWithPort("/proxima/api/jwt/.well-known/jwks.json"),
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertTrue(body.containsKey("keys"));
    }

    @Test
    void shouldUseDefaultValues() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("subject", "minimal@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            createURLWithPort("/proxima/api/jwt/tokens"),
            entity,
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals("HS256", body.get("algorithm"));
        assertEquals("default", body.get("keyId"));
        assertEquals(3600, body.get("expiresIn")); // Default 1 hour
    }
}