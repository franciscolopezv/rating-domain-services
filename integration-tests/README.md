# Integration Tests

This module contains comprehensive integration tests for the Ratings System, covering end-to-end scenarios, error handling, performance testing, and the final acceptance test.

## Test Structure

### 1. RatingsSystemIntegrationTest
- **Purpose**: End-to-end integration testing of the complete ratings flow
- **Coverage**: 
  - gRPC rating submission with database persistence
  - Event publishing and consumption between services
  - GraphQL queries returning updated statistics
  - Concurrent rating submissions and eventual consistency
  - GraphQL federation extension testing

### 2. ErrorScenariosIntegrationTest
- **Purpose**: Testing system resilience and error handling
- **Coverage**:
  - Invalid input validation (rating values, product IDs)
  - Database connection failure scenarios
  - Kafka connection failure scenarios
  - GraphQL error handling for non-existent data
  - System recovery after failures
  - Health endpoint verification

### 3. PerformanceIntegrationTest
- **Purpose**: Performance and load testing
- **Coverage**:
  - Concurrent rating submission performance
  - Event processing latency measurement
  - GraphQL query performance with large datasets
  - Sustained load throughput testing

### 4. FinalAcceptanceTest
- **Purpose**: Complete end-to-end acceptance test as per specification
- **Coverage**:
  - Product creation (simulated)
  - Rating submission via gRPC
  - Event projection validation
  - GraphQL query validation
  - Aggregation logic verification
  - Complete user journey from rating to query

## Running Integration Tests

### Prerequisites
- Docker and Docker Compose installed
- Java 17 or higher
- Maven 3.6 or higher

### Option 1: Using the Test Runner Script (Recommended)
```bash
cd integration-tests
./run-integration-tests.sh
```

This script will:
1. Start the required infrastructure (PostgreSQL, Kafka) using Docker Compose
2. Wait for services to be healthy
3. Run all integration tests
4. Generate test reports
5. Clean up the test environment

### Option 2: Manual Execution

1. Start the test infrastructure:
```bash
cd integration-tests
docker-compose -f docker-compose-integration.yml up -d
```

2. Wait for services to be ready (check health status):
```bash
docker-compose -f docker-compose-integration.yml ps
```

3. Run the tests:
```bash
mvn test -Dtest="*IntegrationTest"
```

4. Clean up:
```bash
docker-compose -f docker-compose-integration.yml down -v
```

### Option 3: Using Testcontainers (Automatic)
The tests use Testcontainers to automatically manage Docker containers. Simply run:
```bash
mvn test
```

Testcontainers will automatically:
- Start required containers (PostgreSQL, Kafka)
- Run the tests
- Clean up containers after tests complete

### Option 4: Final Acceptance Test (Shell Script)
Run the complete acceptance test using the shell script:
```bash
# Start services first
docker-compose up -d

# Run the acceptance test
./final-acceptance-test.sh
```

### Option 5: Final Acceptance Test (Python)
Run the complete acceptance test using Python:
```bash
# Start services first
docker-compose up -d

# Run the acceptance test
python3 final_acceptance_test.py
```

## Test Configuration

### Database Configuration
- **Write Database**: PostgreSQL with `ratings_write` schema
- **Read Database**: PostgreSQL with `ratings_read` schema
- **Initialization**: SQL scripts in `src/test/resources/`

### Kafka Configuration
- **Broker**: Confluent Kafka with embedded Zookeeper
- **Topics**: Auto-created during tests
- **Consumer Groups**: Unique per test run

### Service Ports
- **Command Service**: 
  - gRPC: 9090-9092 (varies by test class)
  - HTTP: 8081, 8083, 8085
- **Query Service**: 
  - GraphQL: 8082, 8084, 8086

## Test Data

### Sample Products
- `integration-test-product-1`: Single rating test
- `integration-test-product-2`: Multiple ratings average calculation
- `concurrent-test-product`: Concurrent submission testing
- `performance-test-product`: Performance testing
- `large-dataset-product-*`: Large dataset testing

### Sample Users
- `integration-test-user-*`: Basic integration tests
- `concurrent-user-*`: Concurrent testing
- `perf-user-*`: Performance testing

## Performance Benchmarks

### Expected Performance Metrics
- **Throughput**: > 10 requests/second under sustained load
- **Latency**: 
  - Average gRPC request: < 1000ms
  - Average GraphQL query: < 500ms
  - End-to-end event processing: < 10 seconds
- **Success Rate**: > 95% under concurrent load
- **Concurrent Users**: Support for 20+ concurrent threads

### Load Testing Parameters
- **Concurrent Threads**: 10-20 threads
- **Test Duration**: 30 seconds sustained load
- **Requests per Thread**: 10-20 requests
- **Dataset Size**: Up to 200 ratings across 10 products

## Troubleshooting

### Common Issues

1. **Port Conflicts**
   - Ensure ports 5432-5435, 9090-9093, 8081-8086 are available
   - Stop any existing PostgreSQL or Kafka instances

2. **Docker Issues**
   - Ensure Docker daemon is running
   - Check available disk space for containers
   - Verify Docker Compose version compatibility

3. **Test Timeouts**
   - Increase timeout values in test configuration
   - Check system resources (CPU, memory)
   - Verify network connectivity between containers

4. **Database Connection Issues**
   - Check PostgreSQL container logs: `docker-compose logs postgres-write-test`
   - Verify database initialization scripts
   - Ensure proper wait conditions for service health

5. **Kafka Connection Issues**
   - Check Kafka container logs: `docker-compose logs kafka-test`
   - Verify Zookeeper is healthy before Kafka starts
   - Check topic creation and consumer group configuration

### Debug Mode
Enable debug logging by setting:
```bash
export SPRING_PROFILES_ACTIVE=integration-test
export LOGGING_LEVEL_COM_RATINGS=DEBUG
```

### Test Reports
After running tests, reports are available in:
- `target/surefire-reports/`: XML and TXT test reports
- Console output: Real-time test progress and performance metrics

## Continuous Integration

### CI/CD Integration
Add to your CI pipeline:
```yaml
- name: Run Integration Tests
  run: |
    cd integration-tests
    ./run-integration-tests.sh
```

### Environment Variables
- `TESTCONTAINERS_RYUK_DISABLED=true`: Disable Ryuk in CI environments
- `SPRING_PROFILES_ACTIVE=integration-test`: Use test configuration
- `MAVEN_OPTS=-Xmx2g`: Increase memory for large test suites

## Contributing

When adding new integration tests:

1. Follow the existing test structure and naming conventions
2. Use appropriate `@Order` annotations for test execution sequence
3. Include performance assertions where applicable
4. Add proper cleanup in `@AfterAll` methods
5. Update this README with new test scenarios
6. Ensure tests are deterministic and can run in parallel

## Requirements Coverage

These integration tests cover the following requirements:
- **1.1, 1.2, 1.3**: gRPC rating submission and database persistence
- **2.1, 2.2, 2.3**: Event processing and projection
- **3.1, 3.2**: GraphQL queries and federation
- **4.1, 4.2**: Error handling and resilience
- **4.5**: Performance and concurrent access