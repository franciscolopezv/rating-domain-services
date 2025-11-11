package com.ratings.query.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 * Simple health controller that provides a basic /health endpoint
 * in addition to the more detailed /actuator/health endpoint.
 */
@RestController
public class HealthController {

    private final DataSource dataSource;

    @Autowired
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Simple health check endpoint.
     * Returns 200 OK if the service is healthy, 503 Service Unavailable otherwise.
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            // Simple database connectivity check
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
                
                if (isValid) {
                    Map<String, Object> response = Map.of(
                        "status", "UP",
                        "service", "ratings-query-service",
                        "database", "UP"
                    );
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = Map.of(
                        "status", "DOWN",
                        "service", "ratings-query-service",
                        "database", "DOWN"
                    );
                    return ResponseEntity.status(503).body(response);
                }
            }
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "DOWN",
                "service", "ratings-query-service",
                "database", "DOWN",
                "error", e.getMessage()
            );
            return ResponseEntity.status(503).body(response);
        }
    }
}