package com.ratings.query.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application metrics and monitoring.
 * Defines custom metrics for tracking query service performance.
 */
@Configuration
public class MetricsConfig {

    /**
     * Counter for GraphQL queries processed.
     */
    @Bean
    public Counter graphqlQueryCounter(MeterRegistry meterRegistry) {
        return Counter.builder("graphql.queries.total")
                .description("Total number of GraphQL queries processed")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Counter for GraphQL query errors.
     */
    @Bean
    public Counter graphqlErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("graphql.errors.total")
                .description("Total number of GraphQL query errors")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Timer for GraphQL query execution time.
     */
    @Bean
    public Timer graphqlQueryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("graphql.query.duration")
                .description("GraphQL query execution time")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Counter for Kafka events processed.
     */
    @Bean
    public Counter kafkaEventCounter(MeterRegistry meterRegistry) {
        return Counter.builder("kafka.events.processed.total")
                .description("Total number of Kafka events processed")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Counter for Kafka event processing errors.
     */
    @Bean
    public Counter kafkaEventErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("kafka.events.errors.total")
                .description("Total number of Kafka event processing errors")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Timer for event projection processing time.
     */
    @Bean
    public Timer eventProjectionTimer(MeterRegistry meterRegistry) {
        return Timer.builder("event.projection.duration")
                .description("Event projection processing time")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Counter for database operations.
     */
    @Bean
    public Counter databaseOperationCounter(MeterRegistry meterRegistry) {
        return Counter.builder("database.operations.total")
                .description("Total number of database operations")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Counter for database operation errors.
     */
    @Bean
    public Counter databaseErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("database.errors.total")
                .description("Total number of database errors")
                .tag("service", "query-service")
                .register(meterRegistry);
    }

    /**
     * Timer for database query execution time.
     */
    @Bean
    public Timer databaseQueryTimer(MeterRegistry meterRegistry) {
        return Timer.builder("database.query.duration")
                .description("Database query execution time")
                .tag("service", "query-service")
                .register(meterRegistry);
    }
}