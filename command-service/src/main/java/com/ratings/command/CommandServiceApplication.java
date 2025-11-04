package com.ratings.command;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for the Ratings Command Service.
 * 
 * This service handles gRPC commands for rating submissions and publishes
 * events to Kafka for eventual consistency with the query service.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.ratings.command", "com.ratings.shared"})
public class CommandServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CommandServiceApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}