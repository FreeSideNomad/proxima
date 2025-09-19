package com.freesidenomad.proxima.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freesidenomad.proxima.model.ProximaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;

@Service
public class JsonConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(JsonConfigurationService.class);
    private static final String CONFIG_FILE_PATH = "config.json";
    private static final String LOCAL_CONFIG_FILE_PATH = "config-local.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProximaConfig cachedConfig;
    private long lastModified = 0;

    public ProximaConfig loadConfiguration() {
        // First check for test resources config.json
        try {
            var resource = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH);
            if (resource != null) {
                ProximaConfig config = objectMapper.readValue(resource, ProximaConfig.class);
                logger.info("Configuration loaded from classpath: {}", CONFIG_FILE_PATH);
                return config;
            }
        } catch (IOException e) {
            logger.debug("No configuration found in classpath, checking file system");
        }

        // Determine config file based on environment
        String configFilePath = determineConfigFile();
        File configFile = new File(configFilePath);

        if (!configFile.exists()) {
            logger.error("Config file not found: {}", configFilePath);
            return createDefaultConfig();
        }

        try {
            long currentModified = configFile.lastModified();
            if (cachedConfig == null || currentModified > lastModified) {
                cachedConfig = objectMapper.readValue(configFile, ProximaConfig.class);
                lastModified = currentModified;
                logger.info("Configuration loaded from {}", configFilePath);
            }
            return copyConfig(cachedConfig);
        } catch (IOException e) {
            logger.error("Error loading configuration from {}: {}", configFilePath, e.getMessage());
            return createDefaultConfig();
        }
    }

    private String determineConfigFile() {
        // Check if we're running in Docker by looking for Docker-specific environment indicators
        boolean isDocker = isRunningInDocker();

        if (isDocker) {
            logger.info("Docker environment detected, using config/config.json");
            return "config/" + CONFIG_FILE_PATH;
        } else {
            // Check if local config exists
            File localConfig = new File(LOCAL_CONFIG_FILE_PATH);
            if (localConfig.exists()) {
                logger.info("Local development environment detected, using config-local.json");
                return LOCAL_CONFIG_FILE_PATH;
            } else {
                logger.info("Local config not found, falling back to config.json");
                return CONFIG_FILE_PATH;
            }
        }
    }

    private boolean isRunningInDocker() {
        // Multiple ways to detect Docker environment

        // 1. Check for Docker-specific environment variables
        if (System.getenv("DOCKER_CONTAINER") != null) {
            return true;
        }

        // 2. Check if hostname contains docker patterns
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && (hostname.length() == 12 || hostname.contains("docker"))) {
            return true;
        }

        // 3. Check for /.dockerenv file (most reliable)
        File dockerEnv = new File("/.dockerenv");
        if (dockerEnv.exists()) {
            return true;
        }

        // 4. Check /proc/1/cgroup for docker/container indicators
        try {
            File cgroupFile = new File("/proc/1/cgroup");
            if (cgroupFile.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(cgroupFile.toPath()));
                if (content.contains("docker") || content.contains("/kubepods") || content.contains("containerd")) {
                    return true;
                }
            }
        } catch (IOException | SecurityException e) {
            // Ignore errors - this check is best effort
        }

        return false;
    }

    public void saveConfiguration(ProximaConfig config) throws IOException {
        String configFilePath = determineConfigFile();
        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(new File(configFilePath), config);
        cachedConfig = copyConfig(config);
        lastModified = new File(configFilePath).lastModified();
        logger.info("Configuration saved to {}", configFilePath);
    }

    public boolean isValidRoute(String pathPattern) {
        ProximaConfig config = loadConfiguration();
        return config.getReservedRoutes().stream()
                .noneMatch(reserved -> matchesPattern(pathPattern, reserved));
    }

    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("**")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    private ProximaConfig createDefaultConfig() {
        ProximaConfig config = new ProximaConfig();
        config.getDownstream().setUrl("http://nginx:80");
        config.setActivePreset("admin_user");
        config.setReservedRoutes(Arrays.asList(
            "/api/**", "/actuator/**", "/dashboard/**",
            "/presets/**", "/routes/**", "/status/**"
        ));
        return config;
    }

    private ProximaConfig copyConfig(ProximaConfig original) {
        if (original == null) return null;
        try {
            String json = objectMapper.writeValueAsString(original);
            return objectMapper.readValue(json, ProximaConfig.class);
        } catch (IOException e) {
            logger.warn("Failed to create defensive copy of configuration, returning original", e);
            return original;
        }
    }
}