# Shared Module

This module contains common models, events, DTOs, and utilities shared across the ratings system services.

## Contents

### Events

- **RatingSubmittedEvent** - Event published when a rating is successfully submitted
  - Contains event metadata (ID, type, timestamp)
  - Includes rating data (submission ID, product ID, rating value, user ID)
  - Supports JSON serialization for Kafka messaging

### DTOs (Data Transfer Objects)

- **RatingSubmissionDto** - For transferring rating submission data
- **ProductStatsDto** - For transferring aggregated product statistics

### Entity Interfaces

- **ReviewEntity** - Contract for review entities across services
- **ProductStatsEntity** - Contract for product statistics entities

### Validation

- **RatingValidationUtils** - Common validation logic for ratings data
  - Product ID format validation
  - Rating value range validation (1-5)
  - User ID format validation
  - Review text length validation
  - Text sanitization utilities

### Constants

- **RatingConstants** - System-wide constants
  - Rating constraints (min/max values)
  - Field length limits
  - Event types and topic names
  - Error messages
  - Metric names

## Usage

This module is included as a dependency in both command and query services:

```xml
<dependency>
    <groupId>com.ratings</groupId>
    <artifactId>shared</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Key Features

- **Type Safety** - Strong typing for all data structures
- **Validation** - Built-in validation utilities with consistent error messages
- **Serialization** - Jackson annotations for JSON serialization
- **Immutability** - Event objects are immutable for thread safety
- **Documentation** - Comprehensive JavaDoc for all public APIs

## Dependencies

- Spring Boot Starter (for basic Spring functionality)
- Jackson (for JSON serialization)
- Bean Validation (for annotation-based validation)
- Spring Data JPA (for entity interfaces)