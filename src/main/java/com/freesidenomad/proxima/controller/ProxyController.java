package com.freesidenomad.proxima.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * ProxyController - deprecated in favor of ProxyFilter.
 *
 * The proxy functionality has been moved to ProxyFilter to avoid conflicts
 * with Spring Boot's built-in controllers (Swagger UI, actuator, etc.).
 *
 * This controller is kept for backwards compatibility but no longer handles
 * catch-all routing. The ProxyFilter now handles all proxy routing based on
 * configured routes only.
 */
@RestController
@RequestMapping("/proxima/proxy")
@Deprecated
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);

    @GetMapping("/status")
    public String getStatus() {
        return "Proxy functionality has been moved to ProxyFilter. This controller is deprecated.";
    }
}