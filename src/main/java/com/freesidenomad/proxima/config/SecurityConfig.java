package com.freesidenomad.proxima.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnMissingBean(name = "testJwtDecoder")
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Configure JWT decoder to use our local JWKS endpoint
        return NimbusJwtDecoder
            .withJwkSetUri("http://localhost:8080/proxima/api/jwt/.well-known/jwks.json")
            .build();
    }
}