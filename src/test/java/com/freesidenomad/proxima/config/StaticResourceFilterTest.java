package com.freesidenomad.proxima.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class StaticResourceFilterTest {

    private StaticResourceFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new StaticResourceFilter();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void testDoFilter_CssResource() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/proxima/css/style.css");
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream(outputStream));

        filter.doFilter(request, response, filterChain);

        // When resource doesn't exist, only status is set, not content type
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response, never()).setContentType(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilter_JsResource() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/proxima/js/script.js");
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream(outputStream));

        filter.doFilter(request, response, filterChain);

        // When resource doesn't exist, only status is set, not content type
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response, never()).setContentType(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilter_ImageResource() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/proxima/images/logo.png");
        when(response.getOutputStream()).thenReturn(new TestServletOutputStream(outputStream));

        filter.doFilter(request, response, filterChain);

        // When resource doesn't exist, only status is set, not content type
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(response, never()).setContentType(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilter_NonStaticResource() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/api/test");

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_ProximaApiPath() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/proxima/api/config");

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_RootPath() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/");

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_ProximaUiPath() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/proxima/ui/");

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setContentType(anyString());
        verify(response, never()).setStatus(anyInt());
        verify(filterChain).doFilter(request, response);
    }

    // Helper class for testing ServletOutputStream
    private static class TestServletOutputStream extends jakarta.servlet.ServletOutputStream {
        private final ByteArrayOutputStream outputStream;

        public TestServletOutputStream(ByteArrayOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            // No-op for testing
        }
    }
}