# Command Service

The command service handles write operations for the ratings system via gRPC. It processes rating submissions, validates input, stores data in the write database, and publishes events to Kafka.

## Responsibilities

- Accept rating submission commands via gRPC
- Validate input data (product ID, rating values, user ID)
- Store reviews in PostgreSQL write database
- Publish RatingSubmittedEvent to Kafka
- Handle command-side business logic and error handling

## Technology Stack

- **Spring Boot 3.2** - Application framework
- **gRPC** - API protocol
- **Spring Data JPA** - Database access
- **PostgreSQL** - Write database
- **Apache Kafka** - Event publishing
- **Spring Actuator** - Health checks and metrics

## API

### gRPC Service Definition

```protobuf
service RatingsCommandService {
  rpc SubmitRating (SubmitRatingCommand) returns (SubmitRatingResponse);
}

message SubmitRatingCommand {
  string product_id = 1;
  int32 rating = 2;
  string user_id = 3;      // Optional
  string review_text = 4;  // Optional
}

message SubmitRatingResponse {
  string submission_id = 1;
  string status = 2;
  string message = 3;      // Error details if needed
}
```

### Endpoints

- **gRPC Server**: Port 9090
- **HTTP Health Check**: Port 8081
  - `/health` - Service health
  - `/ready` - Readiness probe
  - `/metrics` - Prometheus metrics

## Configuration

### Application Properties

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ratings_write
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
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# gRPC Configuration
grpc:
  server:
    port: 9090

# HTTP Server Configuration
server:
  port: 8081
```

### Environment Variables

- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker addresses
- `SPRING_PROFILES_ACTIVE` - Active Spring profile (dev/staging/prod)

## Database Schema

The service uses the following table in the write database:

```sql
CREATE TABLE reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    user_id VARCHAR(255),
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
```

## Error Handling

The service implements comprehensive error handling:

### gRPC Status Codes

- `OK` - Successful rating submission
- `INVALID_ARGUMENT` - Invalid input data (rating out of range, invalid product ID)
- `INTERNAL` - Database or Kafka connection failures
- `UNAVAILABLE` - Service temporarily unavailable

### Validation Rules

- Product ID: Required, alphanumeric, max 255 characters
- Rating: Required, integer between 1-5
- User ID: Optional, alphanumeric if provided, max 255 characters
- Review Text: Optional, max 2000 characters if provided

## Event Publishing

When a rating is successfully stored, the service publishes a `RatingSubmittedEvent` to Kafka:

```json
{
  "eventId": "uuid",
  "eventType": "RatingSubmittedEvent",
  "timestamp": "2023-12-01T10:00:00Z",
  "submissionId": "uuid",
  "productId": "product-123",
  "rating": 5,
  "userId": "user-456"
}
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
docker build -t ratings-command-service .

# Run container
docker run -p 8081:8081 -p 9090:9090 \
  -e DB_USERNAME=ratings_user \
  -e DB_PASSWORD=ratings_pass \
  ratings-command-service
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

The integration tests use Testcontainers to spin up PostgreSQL and Kafka for realistic testing.

## Monitoring

### Health Checks

- `/health` - Overall service health including database and Kafka connectivity
- `/ready` - Readiness probe for Kubernetes deployments

### Metrics

The service exposes Prometheus metrics:

- `ratings_submissions_total` - Total number of rating submissions
- `ratings_submissions_duration` - Rating submission processing time
- `database_connections_active` - Active database connections
- `kafka_producer_records_sent_total` - Kafka messages sent

### Logging

Structured logging includes:

- Request/response logging for gRPC calls
- Database operation logs
- Event publishing logs
- Error and exception tracking