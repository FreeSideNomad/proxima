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
@RequestMapping("/proxy")
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

        String path = request.getRequestURI().substring("/proxy".length());
        String queryString = request.getQueryString();

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
}