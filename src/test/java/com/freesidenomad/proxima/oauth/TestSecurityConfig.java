package com.freesidenomad.proxima.oauth;

import com.freesidenomad.proxima.service.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Allow access to JWKS endpoint (needed for OAuth discovery)
                .requestMatchers("/proxima/api/jwt/.well-known/jwks.json").permitAll()
                // Allow access to JWT management endpoints
                .requestMatchers("/proxima/api/jwt/**").permitAll()
                // Allow access to Proxima admin UI
                .requestMatchers("/proxima/**").permitAll()
                // Allow access to actuator endpoints
                .requestMatchers("/actuator/**").permitAll()
                // Allow access to Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Allow access to public test endpoints
                .requestMatchers("/test/public").permitAll()
                // Secure test endpoint for JWT validation demo
                .requestMatchers("/test/secured/**").authenticated()
                // All other requests are permitted (proxy functionality)
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(testJwtDecoder()))
            );

        return http.build();
    }

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                try {
                    // For testing, parse JWT manually to extract claims without verification
                    String[] parts = token.split("\\.");
                    if (parts.length != 3) {
                        throw new JwtException("Invalid JWT format");
                    }

                    // Decode payload (second part)
                    byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    String payloadJson = new String(payloadBytes);

                    // Parse JSON to extract claims
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> claimsMap = mapper.readValue(payloadJson, Map.class);

                    String subject = (String) claimsMap.get("sub");
                    Long iatLong = ((Number) claimsMap.get("iat")).longValue();
                    Long expLong = ((Number) claimsMap.get("exp")).longValue();

                    Instant issuedAt = Instant.ofEpochSecond(iatLong);
                    Instant expiresAt = Instant.ofEpochSecond(expLong);

                    // Remove standard claims that will be set explicitly
                    claimsMap.remove("sub");
                    claimsMap.remove("iat");
                    claimsMap.remove("exp");

                    // Convert to Spring Security JWT
                    return Jwt.withTokenValue(token)
                        .header("alg", "RS256")
                        .header("kid", "default")
                        .subject(subject)
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .claims(claims -> claims.putAll(claimsMap))
                        .build();
                } catch (Exception e) {
                    throw new JwtException("Invalid JWT token", e);
                }
            }
        };
    }
}