#!/bin/bash
# Server Management Script for GeoBrot Plugin
# Compatible with macOS and Linux

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${SCRIPT_DIR}/server"
PLUGIN_JAR="${SCRIPT_DIR}/target/geobrot-0.1.1.jar"
SERVER_JAR="${SERVER_DIR}/paper.jar"
WORLD_DIR="${SERVER_DIR}/world"
PLUGINS_DIR="${SERVER_DIR}/plugins"
LOGS_DIR="${SERVER_DIR}/logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
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

print_status() {
    log "$1"
}

print_error() {
    error "$1"
}

print_success() {
    log "$1"
}

# Check if server is running
is_server_running() {
    if [[ -f "${SERVER_DIR}/server.pid" ]]; then
        local pid=$(cat "${SERVER_DIR}/server.pid")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        else
            rm -f "${SERVER_DIR}/server.pid"
            return 1
        fi
    fi
    return 1
}

# Setup server environment
setup_server() {
    print_status "Setting up GeoBrot plugin server environment..."
    
    # Create directories
    mkdir -p "$SERVER_DIR" "$PLUGINS_DIR" "$LOGS_DIR"
    
    # Download Paper if not exists
    if [[ ! -f "$SERVER_JAR" ]]; then
        print_status "Downloading Paper server..."
        curl -s -o "$SERVER_JAR" "https://api.papermc.io/v2/projects/paper/versions/1.21.6/builds/latest/downloads/paper-1.21.6-latest.jar"
    fi
    
    # Accept EULA
    echo "eula=true" > "$SERVER_DIR/eula.txt"
    
    # Create server.properties
    cat > "$SERVER_DIR/server.properties" << EOF
# GeoBrot Plugin Test Server Configuration
server-name=GeoBrot Test Server
server-port=25565
online-mode=false
enable-command-block=true
gamemode=creative
difficulty=peaceful
spawn-protection=0
max-players=10
view-distance=10
simulation-distance=10
enable-command-block=true
op-permission-level=4
motd=GeoBrot Fractal World Plugin Test Server
EOF

    print_success "Server environment setup complete"
}

# Build and install plugin
install_plugin() {
    if [[ ! -f "$PLUGIN_JAR" ]]; then
        print_error "Plugin JAR not found. Run 'make build' first."
        exit 1
    fi
    
    print_status "Installing plugin..."
    cp "$PLUGIN_JAR" "$PLUGINS_DIR/"
    print_success "Plugin installed successfully"
}

# Start server
start_server() {
    if is_server_running; then
        print_error "Server is already running!"
        return 1
    fi
    
    if [[ ! -f "$SERVER_JAR" ]]; then
        print_error "Server JAR not found. Run 'make setup' first."
        exit 1
    fi
    
    print_status "Starting server..."
    cd "$SERVER_DIR"
    
    # Start server in background
    nohup java -Xms${MIN_RAM:-2G} -Xmx${MAX_RAM:-4G} \
        -jar "$SERVER_JAR" \
        --nogui \
        > "${LOGS_DIR}/server.log" 2>&1 &
    
    echo $! > "${SERVER_DIR}/server.pid"
    
    print_success "Server started with PID $!"
    print_status "Use 'make attach' to connect to server console"
    print_status "Use 'make logs' to view server logs"
}

# Stop server
stop_server() {
    if ! is_server_running; then
        print_error "Server is not running!"
        return 1
    fi
    
    local pid=$(cat "${SERVER_DIR}/server.pid")
    print_status "Stopping server (PID: $pid)..."
    
    # Send stop command via screen if available
    if command -v screen >/dev/null 2>&1; then
        screen -S minecraft -p 0 -X stuff "stop^M" 2>/dev/null || true
    fi
    
    # Wait for graceful shutdown
    sleep 5
    
    # Force kill if still running
    if ps -p "$pid" > /dev/null 2>&1; then
        kill "$pid"
        sleep 2
    fi
    
    # Force kill if still running
    if ps -p "$pid" > /dev/null 2>&1; then
        kill -9 "$pid"
    fi
    
    rm -f "${SERVER_DIR}/server.pid"
    print_success "Server stopped"
}

# Restart server
restart_server() {
    print_status "Restarting server..."
    if is_server_running; then
        stop_server
        sleep 2
    fi
    start_server
}

# Reset server (clean start)
reset_server() {
    print_status "Resetting server..."
    
    if is_server_running; then
        stop_server
    fi
    
    # Remove world files but keep plugins
    rm -rf "${SERVER_DIR}/world" "${SERVER_DIR}/world_nether" "${SERVER_DIR}/world_the_end"
    rm -rf "${SERVER_DIR}/cache" "${SERVER_DIR}/logs"
    
    print_success "Server reset complete"
}

# Clean server files
clean_server() {
    print_status "Cleaning server files..."
    
    if is_server_running; then
        stop_server
    fi
    
    rm -rf "$SERVER_DIR"
    print_success "Server files cleaned"
}

# Show server status
show_status() {
    print_status "GeoBrot Plugin Server Status"
    echo "==============================="
    
    if is_server_running; then
        local pid=$(cat "${SERVER_DIR}/server.pid")
        echo -e "${GREEN}Status:${NC} Running (PID: $pid)"
        
        if command -v ps >/dev/null 2>&1; then
            echo -e "${BLUE}Memory usage:${NC} $(ps -p "$pid" -o rss= 2>/dev/null | awk '{print int($1/1024)"MB"}' || echo "N/A")"
        fi
        
        # Check if server is responding
        if command -v nc >/dev/null 2>&1; then
            if nc -z localhost 25565 2>/dev/null; then
                echo -e "${GREEN}Port 25565:${NC} Open"
            else
                echo -e "${RED}Port 25565:${NC} Closed"
            fi
        fi
    else
        echo -e "${RED}Status:${NC} Not running"
    fi
    
    echo -e "${BLUE}Plugin JAR:${NC} $([ -f "$PLUGIN_JAR" ] && echo "Available" || echo "Not found")"
    echo -e "${BLUE}Server JAR:${NC} $([ -f "$SERVER_JAR" ] && echo "Available" || echo "Not found")"
    echo -e "${BLUE}Plugins directory:${NC} $([ -d "$PLUGINS_DIR" ] && echo "$(ls -1 "$PLUGINS_DIR" | wc -l) plugins" || echo "Not found")"
}

# Show server logs
show_logs() {
    local log_file="${LOGS_DIR}/server.log"
    
    if [[ -f "$log_file" ]]; then
        tail -n 50 "$log_file"
    else
        print_error "No log file found at $log_file"
    fi
}

# Attach to server console
attach_server() {
    if ! is_server_running; then
        print_error "Server is not running!"
        return 1
    fi
    
    print_status "Attaching to server console..."
    print_status "Use Ctrl+C to detach from console"
    
    local log_file="${LOGS_DIR}/server.log"
    if [[ -f "$log_file" ]]; then
        tail -f "$log_file"
    else
        print_error "No log file found"
    fi
}

# Show network configuration
show_network() {
    print_status "Network Configuration"
    echo "====================="
    
    # Get local IP addresses
    if command -v ip >/dev/null 2>&1; then
        echo -e "${BLUE}Local IP addresses:${NC}"
        ip addr show | grep -E "inet [0-9]" | grep -v "127.0.0.1" | awk '{print "  " $2}' | cut -d'/' -f1
    elif command -v ifconfig >/dev/null 2>&1; then
        echo -e "${BLUE}Local IP addresses:${NC}"
        ifconfig | grep -E "inet [0-9]" | grep -v "127.0.0.1" | awk '{print "  " $2}'
    fi
    
    echo -e "${BLUE}Server port:${NC} 25565"
    echo -e "${BLUE}Connection:${NC} <IP>:25565"
    
    # Check if port is open
    if command -v nc >/dev/null 2>&1; then
        if nc -z localhost 25565 2>/dev/null; then
            echo -e "${GREEN}Port status:${NC} Open"
        else
            echo -e "${RED}Port status:${NC} Closed"
        fi
    fi
}

# Show online players
show_players() {
    print_status "Online Players"
    echo "=============="
    
    if ! is_server_running; then
        print_error "Server is not running!"
        return 1
    fi
    
    # Try to get player count from server log
    local log_file="${LOGS_DIR}/server.log"
    if [[ -f "$log_file" ]]; then
        local player_count=$(grep -c "joined the game" "$log_file" 2>/dev/null || echo "0")
        echo -e "${BLUE}Estimated players online:${NC} $player_count"
    else
        echo "Unable to determine player count"
    fi
}

# Main command handler
case "${1:-help}" in
    setup)
        setup_server
        ;;
    install)
        install_plugin
        ;;
    start)
        start_server
        ;;
    stop)
        stop_server
        ;;
    restart)
        restart_server
        ;;
    reset)
        reset_server
        ;;
    clean)
        clean_server
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    attach)
        attach_server
        ;;
    network)
        show_network
        ;;
    players)
        show_players
        ;;
    help|*)
        echo "GeoBrot Plugin Server Manager"
        echo "Usage: $0 {setup|install|start|stop|restart|reset|clean|status|logs|attach|network|players|help}"
        echo ""
        echo "Commands:"
        echo "  setup     - Set up server environment"
        echo "  install   - Install/update plugin"
        echo "  start     - Start the server"
        echo "  stop      - Stop the server"
        echo "  restart   - Restart the server"
        echo "  reset     - Reset server (clean world)"
        echo "  clean     - Clean all server files"
        echo "  status    - Show server status"
        echo "  logs      - Show server logs"
        echo "  attach    - Attach to server console"
        echo "  network   - Show network configuration"
        echo "  players   - Show online players"
        echo "  help      - Show this help message"
        ;;
esac
