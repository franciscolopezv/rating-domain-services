#!/bin/bash

# Final Acceptance Test Runner for Ratings Microservice System
# This script starts the complete system and runs the final acceptance test

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}$1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${PURPLE}ℹ️  $1${NC}"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to wait for service health
wait_for_service() {
    local service_name=$1
    local max_attempts=60
    local attempt=1
    
    print_step "Waiting for $service_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if $DOCKER_COMPOSE ps --services --filter "status=running" | grep -q "$service_name"; then
            if $DOCKER_COMPOSE exec -T "$service_name" echo "Service is running" >/dev/null 2>&1; then
                print_success "$service_name is healthy!"
                return 0
            fi
        fi
        echo "Attempt $attempt/$max_attempts: $service_name not healthy yet..."
        sleep 5
        ((attempt++))
    done
    
    print_error "$service_name failed to become healthy within expected time"
    return 1
}

# Function to check service endpoint
check_endpoint() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    print_step "Checking $service_name endpoint: $url"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" >/dev/null 2>&1; then
            print_success "$service_name endpoint is responding!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: $service_name endpoint not ready..."
        sleep 2
        ((attempt++))
    done
    
    print_warning "$service_name endpoint not responding, but continuing with test..."
    return 1
}

# Function to cleanup on exit
cleanup() {
    if [ "$CLEANUP_ON_EXIT" = "true" ]; then
        print_step "Cleaning up Docker containers..."
        $DOCKER_COMPOSE down
        print_success "Cleanup completed"
    fi
}

# Set trap for cleanup
trap cleanup EXIT

# Main execution
main() {
    echo "🚀 Final Acceptance Test Runner for Ratings Microservice System"
    echo "=============================================================="
    echo
    
    # Check prerequisites
    print_step "Checking prerequisites..."
    
    if ! command_exists docker; then
        print_error "Docker is required but not installed"
        exit 1
    fi
    
    if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
        print_error "Docker Compose is required but not installed"
        exit 1
    fi
    
    # Use docker compose (v2) if available, otherwise docker-compose (v1)
    if docker compose version >/dev/null 2>&1; then
        DOCKER_COMPOSE="docker compose"
    else
        DOCKER_COMPOSE="docker-compose"
    fi
    
    if ! command_exists curl; then
        print_error "curl is required but not installed"
        exit 1
    fi
    
    if ! command_exists python3; then
        print_warning "python3 not found - will try shell script instead"
    fi
    
    print_success "Prerequisites check completed"
    echo
    
    # Build and start infrastructure services
    print_step "Starting infrastructure services (PostgreSQL, Kafka, etc.)..."
    $DOCKER_COMPOSE up -d postgres-write postgres-read zookeeper kafka redis
    
    # Wait for infrastructure to be ready
    print_step "Waiting for infrastructure services to be healthy..."
    sleep 10
    
    # Check infrastructure health
    print_step "Checking infrastructure health..."
    $DOCKER_COMPOSE ps
    echo
    
    # Build application services
    print_step "Building application services..."
    $DOCKER_COMPOSE build command-service query-service
    
    # Start application services
    print_step "Starting application services..."
    $DOCKER_COMPOSE --profile services up -d
    
    # Wait for services to be ready
    print_step "Waiting for application services to start..."
    sleep 30
    
    # Check service health
    print_step "Checking service health..."
    $DOCKER_COMPOSE ps
    echo
    
    # Check service endpoints
    check_endpoint "http://localhost:8081/actuator/health" "Command Service"
    check_endpoint "http://localhost:8082/actuator/health" "Query Service"
    echo
    
    # Show service logs (last 20 lines)
    print_step "Recent service logs:"
    echo "--- Command Service Logs ---"
    $DOCKER_COMPOSE logs --tail=20 command-service
    echo
    echo "--- Query Service Logs ---"
    $DOCKER_COMPOSE logs --tail=20 query-service
    echo
    
    # Run the final acceptance test
    print_step "Running Final Acceptance Test..."
    echo
    
    # Try Python script first, then shell script
    if command_exists python3 && [ -f "integration-tests/final_acceptance_test.py" ]; then
        print_info "Running Python acceptance test..."
        cd integration-tests
        python3 final_acceptance_test.py
        test_result=$?
        cd ..
    elif [ -f "integration-tests/final-acceptance-test.sh" ]; then
        print_info "Running shell acceptance test..."
        cd integration-tests
        ./final-acceptance-test.sh
        test_result=$?
        cd ..
    else
        print_error "No acceptance test script found!"
        exit 1
    fi
    
    echo
    print_step "=============================================================="
    
    if [ $test_result -eq 0 ]; then
        print_success "🎉 FINAL ACCEPTANCE TEST COMPLETED SUCCESSFULLY!"
        echo
        print_info "System Status:"
        echo "✅ Infrastructure services running"
        echo "✅ Command service (gRPC) running on port 9090"
        echo "✅ Query service (GraphQL) running on port 8082"
        echo "✅ All acceptance criteria validated"
        echo
        print_info "You can now:"
        echo "- View Kafka UI at: http://localhost:8080"
        echo "- Access GraphQL playground at: http://localhost:8082/graphiql"
        echo "- Check service health at: http://localhost:8081/actuator/health"
        echo "- Check service health at: http://localhost:8082/actuator/health"
        echo
        print_info "To stop the system: $DOCKER_COMPOSE down"
    else
        print_error "Final acceptance test failed!"
        echo
        print_info "Debugging information:"
        echo "- Check service logs: $DOCKER_COMPOSE logs [service-name]"
        echo "- Check service status: $DOCKER_COMPOSE ps"
        echo "- Check endpoints manually:"
        echo "  - Command service health: curl http://localhost:8081/actuator/health"
        echo "  - Query service health: curl http://localhost:8082/actuator/health"
        echo "  - GraphQL endpoint: curl -X POST http://localhost:8082/graphql -H 'Content-Type: application/json' -d '{\"query\":\"{__schema{types{name}}}\"}}'"
        exit 1
    fi
}

# Parse command line arguments
CLEANUP_ON_EXIT="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        --cleanup)
            CLEANUP_ON_EXIT="true"
            shift
            ;;
        --help)
            echo "Usage: $0 [--cleanup] [--help]"
            echo "  --cleanup  Clean up Docker containers on exit"
            echo "  --help     Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Run main function
main "$@"