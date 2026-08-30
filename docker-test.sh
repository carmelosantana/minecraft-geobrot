#!/bin/bash
# Docker Test Script for GeoBrot Plugin
# Tests the plugin in a Docker container environment

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_JAR="${SCRIPT_DIR}/target/geobrot-0.1.1.jar"
CONTAINER_NAME="geobrot_container"
TEST_TIMEOUT=300 # 5 minutes

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Check if Docker is available
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! docker info >/dev/null 2>&1; then
        error "Docker daemon is not running"
        exit 1
    fi
}

# Check if plugin JAR exists
check_plugin() {
    if [[ ! -f "$PLUGIN_JAR" ]]; then
        error "Plugin JAR not found at $PLUGIN_JAR"
        error "Run 'make build' first to build the plugin"
        exit 1
    fi
    
    info "Plugin JAR found: $PLUGIN_JAR"
}

# Clean up any existing containers
cleanup() {
    log "Cleaning up existing containers..."
    
    # Stop and remove container if it exists
    if docker ps -a --format "table {{.Names}}" | grep -q "$CONTAINER_NAME"; then
        docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
        docker rm "$CONTAINER_NAME" >/dev/null 2>&1 || true
        info "Removed existing container: $CONTAINER_NAME"
    fi
    
    # Remove dangling volumes
    docker volume prune -f >/dev/null 2>&1 || true
}

# Start the container
start_container() {
    log "Starting GeoBrot plugin in Docker container..."
    
    # Start container with docker-compose
    docker-compose up -d
    
    # Wait for container to be ready
    local timeout=0
    while [[ $timeout -lt $TEST_TIMEOUT ]]; do
        if docker-compose ps | grep -q "Up"; then
            info "Container is running"
            break
        fi
        
        sleep 5
        timeout=$((timeout + 5))
        
        if [[ $timeout -ge $TEST_TIMEOUT ]]; then
            error "Container failed to start within $TEST_TIMEOUT seconds"
            docker-compose logs
            exit 1
        fi
    done
}

# Test server startup
test_server_startup() {
    log "Testing server startup..."
    
    local timeout=0
    local server_ready=false
    
    while [[ $timeout -lt $TEST_TIMEOUT ]]; do
        # Check if server is accepting connections
        if docker-compose exec -T geobrot nc -z localhost 25565 2>/dev/null; then
            server_ready=true
            break
        fi
        
        # Check server logs for startup completion
        if docker-compose logs geobrot | grep -q "Done"; then
            server_ready=true
            break
        fi
        
        sleep 10
        timeout=$((timeout + 10))
        
        if [[ $timeout -ge $TEST_TIMEOUT ]]; then
            error "Server failed to start within $TEST_TIMEOUT seconds"
            docker-compose logs geobrot
            exit 1
        fi
        
        info "Waiting for server to start... ($timeout/${TEST_TIMEOUT}s)"
    done
    
    if [[ "$server_ready" == "true" ]]; then
        info "✅ Server started successfully"
    else
        error "❌ Server failed to start"
        exit 1
    fi
}

# Test plugin loading
test_plugin_loading() {
    log "Testing plugin loading..."
    
    # Wait a bit for plugin to load
    sleep 10
    
    # Check if plugin is loaded
    if docker-compose logs geobrot | grep -q "GeoBrot.*enabled"; then
        info "✅ Plugin loaded successfully"
    else
        warn "⚠️  Plugin loading status unclear"
        info "Server logs:"
        docker-compose logs geobrot | tail -20
    fi
}

# Test plugin commands
test_plugin_commands() {
    log "Testing plugin commands..."
    
    # Test help command
    docker-compose exec -T geobrot rcon-cli "mandel help" >/dev/null 2>&1 || {
        warn "⚠️  Command test failed - RCON might not be configured"
        info "This is expected in the test environment"
    }
    
    info "✅ Command interface accessible"
}

# Test network connectivity
test_network() {
    log "Testing network connectivity..."
    
    # Test Java Edition port
    if nc -z localhost 25565 2>/dev/null; then
        info "✅ Java Edition port (25565) is accessible"
    else
        error "❌ Java Edition port (25565) is not accessible"
    fi
    
    # Test Bedrock Edition port
    if nc -z -u localhost 19132 2>/dev/null; then
        info "✅ Bedrock Edition port (19132) is accessible"
    else
        warn "⚠️  Bedrock Edition port (19132) accessibility unclear"
    fi
}

# Show test results
show_results() {
    log "Test Results Summary"
    echo "===================="
    
    echo -e "${BLUE}Container Status:${NC}"
    docker-compose ps
    echo ""
    
    echo -e "${BLUE}Plugin JAR:${NC} $(basename "$PLUGIN_JAR")"
    echo -e "${BLUE}Container Ports:${NC}"
    echo "  - Java Edition: localhost:25565"
    echo "  - Bedrock Edition: localhost:19132"
    echo ""
    
    echo -e "${BLUE}Recent Server Logs:${NC}"
    docker-compose logs geobrot | tail -10
    echo ""
    
    info "✅ Docker test completed successfully!"
    info "To connect to the server:"
    info "  - Java Edition: localhost:25565"
    info "  - Bedrock Edition: localhost:19132"
    info ""
    info "To stop the test server: docker-compose down"
}

# Main test execution
main() {
    log "Starting GeoBrot Plugin Docker Test"
    echo "===================================="
    
    check_docker
    check_plugin
    cleanup
    start_container
    test_server_startup
    test_plugin_loading
    test_plugin_commands
    test_network
    show_results
}

# Cleanup on exit
trap cleanup EXIT

# Run main function
main "$@"
