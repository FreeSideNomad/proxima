package com.freesidenomad.proxima.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class UrlServiceTest {

    private UrlService urlService;

    @Mock
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        urlService = new UrlService();
    }

    @Test
    void shouldBuildBaseUrlFromRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8080);

        String baseUrl = urlService.getBaseUrl(mockRequest);
        assertEquals("http://localhost:8080", baseUrl);
    }

    @Test
    void shouldBuildBaseUrlWithCustomPort() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("example.com");
        mockRequest.setServerPort(9090);

        String baseUrl = urlService.getBaseUrl(mockRequest);
        assertEquals("http://example.com:9090", baseUrl);
    }

    @Test
    void shouldBuildUrlWithPath() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("https");
        mockRequest.setServerName("api.example.com");
        mockRequest.setServerPort(443);

        String url = urlService.buildUrl(mockRequest, "/oauth/authorize");
        assertEquals("https://api.example.com/oauth/authorize", url);
    }

    @Test
    void shouldBuildDefaultOidcRedirectUri() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8080);

        String redirectUri = urlService.getDefaultOidcRedirectUri(mockRequest);
        assertEquals("http://localhost:8080/proxima/ui/oidc-testing/callback", redirectUri);
    }

    @Test
    void shouldHandleHttpsDefaultPort() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("https");
        mockRequest.setServerName("secure.example.com");
        mockRequest.setServerPort(443);

        String baseUrl = urlService.getBaseUrl(mockRequest);
        assertEquals("https://secure.example.com", baseUrl);
    }

    @Test
    void shouldDetectRequestContext() {
        // When no request context is available
        assertFalse(urlService.hasRequestContext());
    }

    @Test
    void shouldProvideDefaultFallback() {
        // When no request context, should provide fallback URLs
        String baseUrl = urlService.getBaseUrl();
        assertTrue(baseUrl.contains("localhost:8080"));

        String contextUrl = urlService.getContextUrl();
        assertTrue(contextUrl.contains("localhost:8080"));
    }
}