package com.freesidenomad.proxima.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class StaticResourceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        // Handle Proxima static resources
        if (path.startsWith("/proxima/css/")) {
            serveStaticResource(httpResponse, "static/css/" + path.substring("/proxima/css/".length()), "text/css");
            return;
        } else if (path.startsWith("/proxima/js/")) {
            serveStaticResource(httpResponse, "static/js/" + path.substring("/proxima/js/".length()), "application/javascript");
            return;
        } else if (path.startsWith("/proxima/images/")) {
            serveStaticResource(httpResponse, "static/images/" + path.substring("/proxima/images/".length()), "image/png");
            return;
        }

        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    private void serveStaticResource(HttpServletResponse response, String resourcePath, String contentType)
            throws IOException {

        Resource resource = new ClassPathResource(resourcePath);

        if (resource.exists()) {
            response.setContentType(contentType);
            response.setStatus(HttpServletResponse.SC_OK);

            try (InputStream inputStream = resource.getInputStream()) {
                StreamUtils.copy(inputStream, response.getOutputStream());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}