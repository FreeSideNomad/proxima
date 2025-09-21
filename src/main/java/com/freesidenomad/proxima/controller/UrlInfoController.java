package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.service.UrlService;
import com.freesidenomad.proxima.service.OidcClientServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/proxima/api/url-info")
@Tag(name = "URL Information", description = "Dynamic URL detection for the application")
public class UrlInfoController {

    @Autowired
    private UrlService urlService;

    @Autowired
    private OidcClientServiceImpl oidcClientService;

    @GetMapping
    @Operation(
        summary = "Get dynamic URL information",
        description = "Returns the current base URL, context URL, and other dynamic URL information"
    )
    @ApiResponse(responseCode = "200", description = "URL information retrieved successfully")
    public ResponseEntity<Map<String, String>> getUrlInfo(HttpServletRequest request) {

        String baseUrl = urlService.getBaseUrl(request);
        String contextUrl = urlService.getContextUrl(request);
        String defaultRedirectUri = urlService.getDefaultOidcRedirectUri(request);

        // Update self-test client with current URL
        oidcClientService.updateSelfTestClientRedirectUri(baseUrl);

        Map<String, String> urlInfo = Map.of(
            "baseUrl", baseUrl,
            "contextUrl", contextUrl,
            "defaultRedirectUri", defaultRedirectUri,
            "oidcTestingUrl", urlService.buildUrl(request, "/proxima/ui/oidc-testing"),
            "oidcCallbackUrl", urlService.buildUrl(request, "/proxima/ui/oidc-testing/callback"),
            "discoveryUrl", urlService.buildUrl(request, "/.well-known/openid_configuration"),
            "jwksUrl", urlService.buildUrl(request, "/oauth/jwks"),
            "authorizationUrl", urlService.buildUrl(request, "/oauth/authorize"),
            "tokenUrl", urlService.buildUrl(request, "/oauth/token")
        );

        return ResponseEntity.ok(urlInfo);
    }
}