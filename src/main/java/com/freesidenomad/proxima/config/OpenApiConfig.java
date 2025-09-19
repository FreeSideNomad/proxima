package com.freesidenomad.proxima.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Proxima API")
                        .description("JWT Header Injection Reverse Proxy with LCARS Interface")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Proxima Project")
                                .url("https://github.com/FreeSideNomad/proxima")
                                .email("noreply@freesidenomad.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Default Server")))
                .tags(List.of(
                        new Tag()
                                .name("Configuration")
                                .description("Proxy configuration management"),
                        new Tag()
                                .name("Presets")
                                .description("Header preset management"),
                        new Tag()
                                .name("Routes")
                                .description("Route configuration"),
                        new Tag()
                                .name("JWT")
                                .description("JWT token generation and key management"),
                        new Tag()
                                .name("System")
                                .description("System health and monitoring")));
    }
}