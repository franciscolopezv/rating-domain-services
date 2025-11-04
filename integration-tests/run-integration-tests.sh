#!/bin/bash

# Integration Test Runner Script
# This script sets up the test environment and runs integration tests

set -e

echo "=== Ratings System Integration Tests ==="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Clean up any existing containers
print_status "Cleaning up existing test containers..."
docker-compose -f docker-compose-integration.yml down -v --remove-orphans || true

# Start test infrastructure
print_status "Starting test infrastructure (PostgreSQL, Kafka)..."
docker-compose -f docker-compose-integration.yml up -d

# Wait for services to be healthy
print_status "Waiting for services to be ready..."
timeout=60
counter=0

while [ $counter -lt $timeout ]; do
    if docker-compose -f docker-compose-integration.yml ps | grep -q "healthy"; then
        print_status "Services are ready!"
        break
    fi
    
    if [ $counter -eq $((timeout - 1)) ]; then
        print_error "Services failed to start within $timeout seconds"
        docker-compose -f docker-compose-integration.yml logs
        docker-compose -f docker-compose-integration.yml down -v
        exit 1
    fi
    
    sleep 1
    counter=$((counter + 1))
done

# Additional wait to ensure services are fully ready
sleep 5

# Run integration tests
print_status "Running integration tests..."

# Set test environment variables
export SPRING_PROFILES_ACTIVE=integration-test
export TESTCONTAINERS_RYUK_DISABLED=true

# Run tests with Maven
if mvn test -Dtest="*IntegrationTest" -DfailIfNoTests=false; then
    print_status "Integration tests completed successfully!"
    test_result=0
else
    print_error "Integration tests failed!"
    test_result=1
fi

# Generate test report
print_status "Generating test reports..."
if [ -d "target/surefire-reports" ]; then
    echo "Test reports available in: target/surefire-reports/"
    
    # Count test results
    total_tests=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "tests=" {} \; | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
    failed_tests=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "failures=" {} \; | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
    error_tests=$(find target/surefire-reports -name "TEST-*.xml" -exec grep -h "errors=" {} \; | sed 's/.*errors="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
    
    echo "Test Summary:"
    echo "  Total tests: ${total_tests:-0}"
    echo "  Failed tests: ${failed_tests:-0}"
    echo "  Error tests: ${error_tests:-0}"
    echo "  Passed tests: $((${total_tests:-0} - ${failed_tests:-0} - ${error_tests:-0}))"
fi

# Cleanup
print_status "Cleaning up test infrastructure..."
docker-compose -f docker-compose-integration.yml down -v

if [ $test_result -eq 0 ]; then
    print_status "All integration tests passed! ✅"
else
    print_error "Some integration tests failed! ❌"
fi

exit $test_result