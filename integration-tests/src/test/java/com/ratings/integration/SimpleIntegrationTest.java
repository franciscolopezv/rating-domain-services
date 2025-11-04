package com.ratings.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test to verify the test setup works.
 */
@Testcontainers
class SimpleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Test
    void testContainerSetup() {
        assertTrue(postgres.isRunning());
        assertNotNull(postgres.getJdbcUrl());
        System.out.println("PostgreSQL container is running at: " + postgres.getJdbcUrl());
    }

    @Test
    void testBasicFunctionality() {
        // Basic test to verify JUnit and assertions work
        assertEquals(2, 1 + 1);
        assertTrue(true);
        assertNotNull("test");
    }
}