package com.ratings.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the Ratings Query Service.
 * 
 * This service provides GraphQL federation endpoints for querying product
 * rating statistics and consumes events from Kafka to maintain read-optimized
 * data stores.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.ratings.query", "com.ratings.shared"})
public class QueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryServiceApplication.class, args);
    }
}