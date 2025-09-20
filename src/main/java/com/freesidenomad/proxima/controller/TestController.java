package com.freesidenomad.proxima.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        response.put("authenticated", false);
        return response;
    }

    @GetMapping("/secured/user")
    public Map<String, Object> securedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This endpoint requires valid JWT authentication");
        response.put("authenticated", true);
        response.put("subject", jwt.getSubject());
        response.put("claims", jwt.getClaims());
        response.put("issuer", jwt.getIssuer());
        response.put("audience", jwt.getAudience());
        return response;
    }
}