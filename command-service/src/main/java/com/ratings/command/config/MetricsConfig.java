package com.ratings.command.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application metrics.
 */
@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter ratingSubmissionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ratings.submissions.total")
            .description("Total number of rating submissions")
            .register(meterRegistry);
    }
    
    @Bean
    public Timer ratingSubmissionTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ratings.submissions.duration")
            .description("Duration of rating submission processing")
            .register(meterRegistry);
    }
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }
    
    @Bean
    public Counter eventPublishingCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ratings.events.published.total")
            .description("Total number of events published")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter eventPublishingErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ratings.events.publishing.errors.total")
            .description("Total number of event publishing errors")
            .register(meterRegistry);
    }
}