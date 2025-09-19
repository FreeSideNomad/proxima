package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.service.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;

@RestController
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    @Autowired
    private ProxyService proxyService;

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST,
                    RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH,
                    RequestMethod.OPTIONS, RequestMethod.HEAD})
    public CompletableFuture<ResponseEntity<String>> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {

        String path = request.getRequestURI();
        String queryString = request.getQueryString();

        // Exclude static resources from proxying - let Spring Boot handle them
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/webjars/") ||
            path.startsWith("/proxima/css/") || path.startsWith("/proxima/js/") || path.startsWith("/proxima/images/")) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }

        // Exclude reserved Proxima routes - these are handled by other controllers
        if (isReservedProximaRoute(path)) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }

        if (queryString != null) {
            path += "?" + queryString;
        }

        logger.debug("Proxying {} request to path: {}", request.getMethod(), path);

        return proxyService.forwardRequest(request.getMethod(), path, request, body)
                .exceptionally(throwable -> {
                    logger.error("Error proxying request: ", throwable);
                    if (throwable instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) throwable;
                        return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
                    }
                    return ResponseEntity.status(500).body("Internal Server Error");
                });
    }

    private boolean isReservedProximaRoute(String path) {
        return path.startsWith("/proxima/") ||
               path.startsWith("/actuator/") ||
               path.equals("/");
    }
}