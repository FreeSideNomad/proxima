package com.freesidenomad.proxima.controller;

import com.freesidenomad.proxima.model.oidc.OidcClient;
import com.freesidenomad.proxima.service.OidcClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/proxima/api/oidc/clients")
@Tag(name = "OIDC Client Management", description = "Manage OIDC client configurations")
public class OidcClientController {

    private static final Logger logger = LoggerFactory.getLogger(OidcClientController.class);

    @Autowired
    private OidcClientService clientService;

    @GetMapping
    @Operation(
        summary = "List all OIDC clients",
        description = "Retrieve all registered OIDC clients"
    )
    @ApiResponse(responseCode = "200", description = "List of OIDC clients retrieved successfully")
    public ResponseEntity<List<OidcClient>> getAllClients() {
        try {
            List<OidcClient> clients = clientService.getAllClients();
            logger.info("Retrieved {} OIDC clients", clients.size());
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            logger.error("Error retrieving OIDC clients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{clientId}")
    @Operation(
        summary = "Get OIDC client by ID",
        description = "Retrieve a specific OIDC client by its client ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OIDC client found"),
        @ApiResponse(responseCode = "404", description = "OIDC client not found")
    })
    public ResponseEntity<OidcClient> getClient(
            @Parameter(description = "Client ID") @PathVariable String clientId) {
        try {
            Optional<OidcClient> client = clientService.getClientById(clientId);
            if (client.isPresent()) {
                logger.info("Retrieved OIDC client: {}", clientId);
                return ResponseEntity.ok(client.get());
            } else {
                logger.warn("OIDC client not found: {}", clientId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving OIDC client: " + clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @Operation(
        summary = "Register new OIDC client",
        description = "Register a new OIDC client configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "OIDC client registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid client configuration"),
        @ApiResponse(responseCode = "409", description = "Client already exists")
    })
    public ResponseEntity<?> registerClient(@RequestBody OidcClient client) {
        try {
            OidcClient registeredClient = clientService.registerClient(client);
            logger.info("Registered new OIDC client: {}", registeredClient.getClientId());
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredClient);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid OIDC client registration request", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error registering OIDC client", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/{clientId}")
    @Operation(
        summary = "Update OIDC client",
        description = "Update an existing OIDC client configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OIDC client updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid client configuration"),
        @ApiResponse(responseCode = "404", description = "OIDC client not found")
    })
    public ResponseEntity<?> updateClient(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @RequestBody OidcClient client) {
        try {
            OidcClient updatedClient = clientService.updateClient(clientId, client);
            logger.info("Updated OIDC client: {}", clientId);
            return ResponseEntity.ok(updatedClient);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid OIDC client update request for: " + clientId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating OIDC client: " + clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @DeleteMapping("/{clientId}")
    @Operation(
        summary = "Delete OIDC client",
        description = "Delete an OIDC client configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "OIDC client deleted successfully"),
        @ApiResponse(responseCode = "404", description = "OIDC client not found")
    })
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "Client ID") @PathVariable String clientId) {
        try {
            if (clientService.deleteClient(clientId)) {
                logger.info("Deleted OIDC client: {}", clientId);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("OIDC client not found for deletion: {}", clientId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting OIDC client: " + clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{clientId}/enabled")
    @Operation(
        summary = "Enable/disable OIDC client",
        description = "Enable or disable an OIDC client"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OIDC client status updated"),
        @ApiResponse(responseCode = "404", description = "OIDC client not found")
    })
    public ResponseEntity<?> setClientEnabled(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'enabled' field"));
            }

            if (clientService.setClientEnabled(clientId, enabled)) {
                logger.info("Set OIDC client {} enabled status to: {}", clientId, enabled);
                return ResponseEntity.ok(Map.of("clientId", clientId, "enabled", enabled));
            } else {
                logger.warn("OIDC client not found: {}", clientId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating OIDC client status: " + clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Get OIDC client statistics",
        description = "Retrieve statistics about registered OIDC clients"
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<OidcClientService.ClientStats> getStats() {
        try {
            OidcClientService.ClientStats stats = clientService.getStats();
            logger.info("Retrieved OIDC client statistics: {} total, {} enabled",
                stats.totalClients(), stats.enabledClients());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving OIDC client statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}