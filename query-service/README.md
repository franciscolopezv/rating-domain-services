# Query Service

The query service handles read operations for the ratings system via GraphQL federation. It extends the Product entity with rating data, consumes events from Kafka to maintain read-optimized data stores, and provides aggregated statistics.

## Responsibilities

- Extend Product entity with rating data via GraphQL federation
- Resolve rating queries from read database
- Consume RatingSubmittedEvent from Kafka
- Maintain read-optimized product statistics
- Handle query-side business logic and projections

## Technology Stack

- **Spring Boot 3.2** - Application framework
- **GraphQL** - API protocol with Apollo Federation
- **Spring Data JPA** - Database access
- **PostgreSQL** - Read database
- **Apache Kafka** - Event consumption
- **Spring Actuator** - Health checks and metrics

## API

### GraphQL Schema

The service extends the Product type via Apollo Federation:

```graphql
extend type Product @key(fields: "id") {
  id: ID! @external
  averageRating: Float
  reviewCount: Int
  ratingDistribution: RatingDistribution
}

type RatingDistribution {
  oneStar: Int
  twoStar: Int
  threeStar: Int
  fourStar: Int
  fiveStar: Int
}
```

### Endpoints

- **GraphQL API**: `http://localhost:8082/graphql`
- **GraphQL Playground**: `http://localhost:8082/graphiql` (dev profile only)
- **Health Check**: `http://localhost:8082/health`
- **Readiness Check**: `http://localhost:8082/ready`
- **Metrics**: `http://localhost:8082/metrics`

### Example Queries

```graphql
# Query product with rating data
query GetProduct($id: ID!) {
  product(id: $id) {
    id
    name
    averageRating
    reviewCount
    ratingDistribution {
      oneStar
      twoStar
      threeStar
      fourStar
      fiveStar
    }
  }
}
```

## Configuration

### Application Properties

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ratings_read
    username: ${DB_USERNAME:ratings_user}
    password: ${DB_PASSWORD:ratings_pass}
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ratings-projector
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

# GraphQL Configuration
spring:
  graphql:
    federation:
      enabled: true

# HTTP Server Configuration
server:
  port: 8082
```

### Environment Variables

- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker addresses
- `SPRING_PROFILES_ACTIVE` - Active Spring profile (dev/staging/prod)

## Database Schema

The service uses the following table in the read database:

```sql
CREATE TABLE product_stats (
    product_id VARCHAR(255) PRIMARY KEY,
    average_rating DECIMAL(3,2),
    review_count INTEGER DEFAULT 0,
    rating_distribution JSONB,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_stats_average_rating ON product_stats(average_rating);
CREATE INDEX idx_product_stats_review_count ON product_stats(review_count);
```

## Event Processing

The service consumes `RatingSubmittedEvent` messages and updates product statistics:

### Event Flow

1. **Event Consumption** - Kafka listener receives RatingSubmittedEvent
2. **Statistics Calculation** - Recalculate average rating and review count
3. **Distribution Update** - Update rating distribution by star level
4. **Database Update** - Store updated statistics in read database
5. **Error Handling** - Handle failures with retry and dead letter queue

### Projection Logic

```java
@KafkaListener(topics = "ratings_events")
public void handleRatingSubmittedEvent(RatingSubmittedEvent event) {
    // Recalculate product statistics
    // Update rating distribution
    // Store in read database
}
```

## Error Handling

### GraphQL Error Handling

- **Null Values** - Return null for missing data rather than errors
- **Partial Failures** - Continue resolving other fields if one fails
- **Error Extensions** - Include error codes and details in response

### Event Processing Errors

- **Retry Logic** - Exponential backoff for transient failures
- **Dead Letter Queue** - Failed events sent to DLQ for manual review
- **Circuit Breaker** - Prevent cascade failures during database outages
- **Monitoring** - Metrics and alerts for processing failures

## Federation

The service participates in Apollo Federation as a subgraph:

### Key Features

- **Entity Extension** - Extends Product type with rating fields
- **Reference Resolution** - Resolves Product entities by ID
- **Schema Composition** - Participates in federated schema
- **Type Safety** - Strong typing for all GraphQL operations

### Federation Configuration

```yaml
spring:
  graphql:
    federation:
      enabled: true
      schema-printer:
        enabled: true
```

## Running the Service

### Local Development

```bash
# Start dependencies
docker-compose up -d postgres kafka

# Run the service
mvn spring-boot:run
```

### Docker

```bash
# Build image
docker build -t ratings-query-service .

# Run container
docker run -p 8082:8082 \
  -e DB_USERNAME=ratings_user \
  -e DB_PASSWORD=ratings_pass \
  ratings-query-service
```

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify -P integration-tests
```

### GraphQL Testing

The service includes GraphQL-specific tests:

- Schema validation tests
- Resolver unit tests
- Federation integration tests
- End-to-end query tests

## Monitoring

### Health Checks

- `/health` - Overall service health including database and Kafka connectivity
- `/ready` - Readiness probe for Kubernetes deployments

### Metrics

The service exposes Prometheus metrics:

- `ratings_events_processed_total` - Total events processed
- `ratings_events_processing_duration` - Event processing time
- `graphql_queries_total` - Total GraphQL queries
- `graphql_query_duration` - GraphQL query execution time
- `database_connections_active` - Active database connections

### Logging

Structured logging includes:

- GraphQL query logging
- Event processing logs
- Database operation logs
- Error and exception tracking
- Performance metrics