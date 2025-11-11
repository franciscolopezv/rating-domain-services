package com.ratings.query.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Controller for GraphQL Federation service discovery.
 * Provides the /_service endpoint required by Apollo Federation.
 */
@RestController
public class FederationController {

    private final ResourceLoader resourceLoader;
    private final String serviceName;
    private final String serviceUrl;

    public FederationController(
            ResourceLoader resourceLoader,
            @Value("${app.graphql.federation.service-name:ratings-query-service}") String serviceName,
            @Value("${app.graphql.federation.service-url:http://localhost:8082/graphql}") String serviceUrl) {
        this.resourceLoader = resourceLoader;
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
    }

    /**
     * Federation service discovery endpoint.
     * Returns the GraphQL schema definition language (SDL) for this service.
     *
     * @return ResponseEntity with the service schema
     */
    @GetMapping(value = "/_service", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> service() {
        try {
            // Load the GraphQL schema
            Resource schemaResource = resourceLoader.getResource("classpath:graphql/schema.graphqls");
            String sdl = schemaResource.getContentAsString(StandardCharsets.UTF_8);
            
            // Return the federation service response
            Map<String, Object> response = Map.of(
                "sdl", sdl
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            // Return error response if schema cannot be loaded
            Map<String, Object> errorResponse = Map.of(
                "error", "Unable to load GraphQL schema",
                "message", e.getMessage()
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Additional endpoint to provide service information.
     * This can be useful for debugging and service discovery.
     *
     * @return ResponseEntity with service information
     */
    @GetMapping(value = "/_service/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        Map<String, Object> info = Map.of(
            "name", serviceName,
            "url", serviceUrl,
            "type", "graphql-federation-subgraph",
            "version", "1.0.0",
            "capabilities", Map.of(
                "federation", true,
                "subscriptions", false,
                "introspection", true
            )
        );
        
        return ResponseEntity.ok(info);
    }
}