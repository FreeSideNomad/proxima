package com.freesidenomad.proxima.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("oauth-test")
public class OAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testJwksEndpointIsAccessible() throws Exception {
        mockMvc.perform(get("/proxima/api/jwt/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists());
    }

    @Test
    void testPublicEndpointAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/test/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("This is a public endpoint"))
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void testSecuredEndpointRequiresAuth() throws Exception {
        // Without JWT token, should get 401
        mockMvc.perform(get("/test/secured/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testEndToEndJwtValidation() throws Exception {
        // Step 1: Generate a JWT token using our API
        Map<String, Object> tokenRequest = new HashMap<>();
        tokenRequest.put("subject", "test@example.com");
        tokenRequest.put("algorithm", "RS256");
        tokenRequest.put("keyId", "default");
        tokenRequest.put("expirationSeconds", 3600);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "user");
        claims.put("permissions", new String[]{"read", "write"});
        tokenRequest.put("claims", claims);

        MvcResult result = mockMvc.perform(post("/proxima/api/jwt/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);
        String jwtToken = (String) response.get("token");

        // Step 2: Use the generated JWT to access secured endpoint
        mockMvc.perform(get("/test/secured/user")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.subject").value("test@example.com"))
                .andExpect(jsonPath("$.claims.role").value("user"));
    }
}