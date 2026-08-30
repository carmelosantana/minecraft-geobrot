# Makefile for GeoBrot Plugin Server Management
# Provides convenient shortcuts for common development tasks

# Project Configuration
PLUGIN_NAME = geobrot
PLUGIN_VERSION = 0.1.1
MINECRAFT_VERSION = 1.21.6
SERVER_PORT = 25565
MIN_RAM = 2G
MAX_RAM = 4G

# Export environment variables for server-manager.sh
export PLUGIN_NAME PLUGIN_VERSION MINECRAFT_VERSION SERVER_PORT MIN_RAM MAX_RAM

.PHONY: help setup start stop restart reset clean status logs build install test dev docker-build docker-test debug network players version lint format validate release attach check-deps

# Default target
help: ## Show this help message
	@echo "🌀 GeoBrot Plugin - Development Commands"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "Quick Start:"
	@echo "  make setup      # Initial server setup"
	@echo "  make dev        # Quick development cycle (build + install + restart)"
	@echo "  make test       # Run plugin tests"
	@echo "  make attach     # Attach to server console"
	@echo ""
	@echo "Configuration:"
	@echo "  Plugin: $(PLUGIN_NAME) v$(PLUGIN_VERSION)"
	@echo "  Minecraft: $(MINECRAFT_VERSION)"
	@echo "  RAM: $(MIN_RAM) - $(MAX_RAM)"
	@echo "  Port: $(SERVER_PORT)"

# Check if required tools are installed
check-deps: ## Check if required dependencies are installed
	@echo "🔍 Checking dependencies..."
	@command -v java >/dev/null 2>&1 || { echo "❌ Java is required but not installed. Install Java 21+"; exit 1; }
	@command -v mvn >/dev/null 2>&1 || { echo "❌ Maven is required but not installed. Install Maven"; exit 1; }
	@command -v docker >/dev/null 2>&1 || echo "⚠️  Docker not found - docker commands will not work"
	@echo "✅ Core dependencies are installed"

# Project Setup
setup: check-deps ## Set up the development environment
	@echo "🔧 Setting up development environment..."
	@chmod +x server-manager.sh
	@./server-manager.sh setup

# Server Management
start: ## Start the test server
	@./server-manager.sh start

stop: ## Stop the test server
	@./server-manager.sh stop

restart: ## Restart the test server
	@./server-manager.sh restart

reset: ## Reset server (clean start)
	@./server-manager.sh reset

clean: ## Clean server files
	@./server-manager.sh clean

status: ## Show server status
	@./server-manager.sh status

logs: ## Show recent server logs
	@./server-manager.sh logs

build: ## Build the plugin JAR
	@echo "Building plugin..."
	@mvn clean package
	@echo "✅ Plugin built successfully"

install: build ## Install/update plugin to server
	@./server-manager.sh install

test: ## Run plugin tests
	@echo "Running tests..."
	@mvn test
	@echo "✅ Tests completed"

dev: build install restart ## Quick development cycle
	@echo "🚀 Development cycle complete!"

docker-build: build ## Build Docker container with plugin
	@echo "Building Docker container..."
	@docker-compose -f docker-compose.yml build
	@echo "✅ Docker container built"

docker-test: docker-build ## Test plugin in Docker container
	@echo "Testing plugin in Docker container..."
	@./docker-test.sh
	@echo "✅ Docker test completed"

debug: ## Debug plugin functionality in running server
	@echo "=== Debug Commands ==="
	@echo "Running interactive debug script..."
	@./debug-plugin.sh

test-commands: build install ## Build, install and show test commands
	@echo "Plugin installed! Test with these commands:"
	@echo ""
	@echo "=== Player Commands ==="
	@echo "  /mandel help              - Show help"
	@echo "  /mandel list              - List fractal worlds"
	@echo "  /mandel tp <world>        - Teleport to fractal world"
	@echo "  /mandel info <world>      - Show world information"
	@echo ""
	@echo "=== Admin Commands (op required) ==="
	@echo "  /mandel create <name> [seed]  - Create fractal world"
	@echo "  /mandel regen <name>          - Regenerate fractal world"
	@echo ""
	@echo "=== Example Usage ==="
	@echo "  /mandel create myworld spiral    - Create world with spiral seed"
	@echo "  /mandel create testworld         - Create world with random seed"
	@echo "  /mandel tp myworld               - Teleport to myworld"
	@echo ""
	@echo "To test, restart server: make restart"

network: ## Show network configuration for external access
	@./server-manager.sh network

players: ## Show online players
	@./server-manager.sh players

version: ## Show versions of Java, Maven, and plugin
	@echo "=== Version Information ==="
	@echo "Java: $$(java -version 2>&1 | head -n 1)"
	@echo "Maven: $$(mvn -version 2>&1 | head -n 1)"
	@echo "Plugin: $(PLUGIN_NAME) v$(PLUGIN_VERSION)"
	@echo "Minecraft: $(MINECRAFT_VERSION)"

# Code Quality
lint: ## Run code linting
	@echo "Running code linting..."
	@mvn checkstyle:check
	@echo "✅ Linting completed"

format: ## Format code
	@echo "Formatting code..."
	@mvn spotless:apply
	@echo "✅ Code formatted"

validate: lint test ## Run linting and tests
	@echo "✅ Validation completed"

# Release Management
release: clean build test ## Create a release build
	@echo "Creating release..."
	@mkdir -p releases
	@cp target/geobrot-*.jar releases/
	@echo "✅ Release created in releases/ directory"

attach: ## Attach to running server console
	@./server-manager.sh attach

# Health Check
health-check: ## Comprehensive system health check
	@echo "=== System Health Check ==="
	@echo "Java Version:"
	@java -version 2>&1 | head -n 3
	@echo ""
	@echo "Maven Version:"
	@mvn -version 2>&1 | head -n 1
	@echo ""
	@echo "Memory Usage:"
	@free -h 2>/dev/null || echo "Memory check not available on this system"
	@echo ""
	@echo "Disk Space:"
	@df -h . 2>/dev/null || echo "Disk check not available on this system"
	@echo ""
	@echo "Plugin Status:"
	@[ -f "target/geobrot-$(PLUGIN_VERSION).jar" ] && echo "✅ Plugin JAR exists" || echo "❌ Plugin JAR missing"
	@[ -f "server-manager.sh" ] && echo "✅ Server manager ready" || echo "❌ Server manager missing"
	@echo ""
