package com.freesidenomad.proxima.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for dynamic URL detection and construction
 */
@Service
public class UrlService {

    /**
     * Get the current base URL (protocol + host + port)
     */
    public String getBaseUrl() {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                .replacePath(null)
                .build()
                .toString();
        } catch (Exception e) {
            // Fallback to localhost if no request context
            return "http://localhost:8080";
        }
    }

    /**
     * Get the current base URL from request
     */
    public String getBaseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
            .replacePath(null)
            .replaceQuery(null)
            .build()
            .toString();
    }

    /**
     * Get the current application context path URL
     */
    public String getContextUrl() {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toString();
        } catch (Exception e) {
            return "http://localhost:8080";
        }
    }

    /**
     * Get the current application context path URL from request
     */
    public String getContextUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request)
            .build()
            .toString();
    }

    /**
     * Build a full URL from the current request context
     */
    public String buildUrl(String path) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path)
                .build()
                .toString();
        } catch (Exception e) {
            return "http://localhost:8080" + path;
        }
    }

    /**
     * Build a full URL from request
     */
    public String buildUrl(HttpServletRequest request, String path) {
        return ServletUriComponentsBuilder.fromContextPath(request)
            .path(path)
            .build()
            .toString();
    }

    /**
     * Get default OIDC redirect URI for self-testing
     */
    public String getDefaultOidcRedirectUri() {
        return buildUrl("/proxima/ui/oidc-testing/callback");
    }

    /**
     * Get default OIDC redirect URI for self-testing from request
     */
    public String getDefaultOidcRedirectUri(HttpServletRequest request) {
        return buildUrl(request, "/proxima/ui/oidc-testing/callback");
    }

    /**
     * Check if we're in a request context
     */
    public boolean hasRequestContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null && attributes.getRequest() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current request if available
     */
    public HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}