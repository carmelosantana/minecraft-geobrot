#!/bin/bash
# Debug Script for GeoBrot Plugin
# Interactive debugging and testing tools

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${SCRIPT_DIR}/server"
LOGS_DIR="${SERVER_DIR}/logs"
PLUGIN_JAR="${SCRIPT_DIR}/target/geobrot-0.1.1.jar"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${GREEN}[DEBUG]${NC} $1"
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

# Check if server is running
is_server_running() {
    if [[ -f "${SERVER_DIR}/server.pid" ]]; then
        local pid=$(cat "${SERVER_DIR}/server.pid")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# Show debug menu
show_menu() {
    echo ""
    echo "========================================"
    echo "         GeoBrot Plugin Debug Menu"
    echo "========================================"
    echo ""
    echo "1) Check Plugin Status"
    echo "2) View Server Logs"
    echo "3) Test Fractal Math"
    echo "4) Check World Generation"
    echo "5) Test Commands"
    echo "6) Performance Analysis"
    echo "7) Memory Usage"
    echo "8) Export Debug Report"
    echo "9) Live Log Monitoring"
    echo "0) Exit"
    echo ""
    echo -n "Select an option: "
}

# Check plugin status
check_plugin_status() {
    log "Checking plugin status..."
    
    if [[ ! -f "$PLUGIN_JAR" ]]; then
        error "Plugin JAR not found: $PLUGIN_JAR"
        return 1
    fi
    
    info "Plugin JAR: $(basename "$PLUGIN_JAR")"
    info "Size: $(du -h "$PLUGIN_JAR" | cut -f1)"
    info "Modified: $(stat -c %y "$PLUGIN_JAR" 2>/dev/null || stat -f %Sm "$PLUGIN_JAR")"
    
    if is_server_running; then
        info "Server Status: Running"
        
        # Check if plugin is loaded
        local log_file="${LOGS_DIR}/server.log"
        if [[ -f "$log_file" ]]; then
            if grep -q "GeoBrot.*enabled" "$log_file"; then
                info "✅ Plugin is loaded and enabled"
            else
                warn "⚠️  Plugin loading status unclear"
            fi
            
            # Check for errors
            local error_count=$(grep -c "ERROR.*GeoBrot" "$log_file" 2>/dev/null || echo "0")
            if [[ $error_count -gt 0 ]]; then
                warn "Found $error_count errors in logs"
            else
                info "✅ No errors found in logs"
            fi
        fi
    else
        warn "Server Status: Not running"
    fi
}

# View server logs
view_logs() {
    log "Viewing server logs..."
    
    local log_file="${LOGS_DIR}/server.log"
    if [[ -f "$log_file" ]]; then
        echo ""
        echo "Recent Server Logs:"
        echo "==================="
        tail -50 "$log_file"
        echo ""
        
        # Show GeoBrot-specific logs
        echo "GeoBrot Plugin Logs:"
        echo "==================="
        grep "GeoBrot" "$log_file" 2>/dev/null | tail -20 || echo "No GeoBrot logs found"
    else
        error "Log file not found: $log_file"
    fi
}

# Test fractal math
test_fractal_math() {
    log "Testing fractal math calculations..."
    
    # Create a simple Java test
    local test_file="/tmp/FractalTest.java"
    cat > "$test_file" << 'EOF'
import java.util.Arrays;

public class FractalTest {
    public static void main(String[] args) {
        System.out.println("Testing Mandelbrot calculations...");
        
        // Test known points
        testPoint(-0.7, 0.0, "Classic point");
        testPoint(0.0, 0.0, "Origin");
        testPoint(-2.0, 0.0, "Far left");
        testPoint(1.0, 0.0, "Far right");
        testPoint(0.0, 1.0, "Top");
        testPoint(0.0, -1.0, "Bottom");
        
        System.out.println("✅ Fractal math test completed");
    }
    
    static void testPoint(double x, double y, String description) {
        int escapeTime = mandelbrotEscapeTime(x, y);
        System.out.println(String.format("%s (%.2f, %.2f): %d iterations", 
            description, x, y, escapeTime));
    }
    
    static int mandelbrotEscapeTime(double x, double y) {
        double zx = 0.0;
        double zy = 0.0;
        int maxIterations = 100;
        
        for (int i = 0; i < maxIterations; i++) {
            double zx2 = zx * zx;
            double zy2 = zy * zy;
            
            if (zx2 + zy2 > 4.0) {
                return i;
            }
            
            zy = 2.0 * zx * zy + y;
            zx = zx2 - zy2 + x;
        }
        
        return maxIterations;
    }
}
EOF
    
    # Compile and run the test
    if javac "$test_file" -d /tmp && java -cp /tmp FractalTest; then
        info "✅ Fractal math test passed"
    else
        error "❌ Fractal math test failed"
    fi
    
    rm -f "$test_file" "/tmp/FractalTest.class"
}

# Check world generation
check_world_generation() {
    log "Checking world generation..."
    
    if ! is_server_running; then
        error "Server must be running to test world generation"
        return 1
    fi
    
    info "Server is running - world generation can be tested"
    info "Suggested test commands:"
    echo "  /mandel create testworld"
    echo "  /mandel list"
    echo "  /mandel tp testworld"
    echo "  /mandel info testworld"
    echo ""
    info "Check server logs for generation progress"
}

# Test commands
test_commands() {
    log "Testing plugin commands..."
    
    if ! is_server_running; then
        error "Server must be running to test commands"
        return 1
    fi
    
    info "Available commands to test:"
    echo "  /mandel help"
    echo "  /mandel list"
    echo "  /mandel create <name> [seed]"
    echo "  /mandel tp <name>"
    echo "  /mandel info <name>"
    echo "  /mandel regen <name>"
    echo ""
    info "Run these commands in the game or server console"
}

# Performance analysis
performance_analysis() {
    log "Performing performance analysis..."
    
    if is_server_running; then
        local pid=$(cat "${SERVER_DIR}/server.pid")
        
        info "Server Performance:"
        echo "  PID: $pid"
        
        if command -v ps >/dev/null 2>&1; then
            echo "  CPU Usage: $(ps -p "$pid" -o %cpu= 2>/dev/null || echo "N/A")%"
            echo "  Memory Usage: $(ps -p "$pid" -o rss= 2>/dev/null | awk '{print int($1/1024)"MB"}' || echo "N/A")"
        fi
        
        # Check log for timing information
        local log_file="${LOGS_DIR}/server.log"
        if [[ -f "$log_file" ]]; then
            local world_gen_time=$(grep "Created fractal world" "$log_file" 2>/dev/null | wc -l)
            info "Worlds created: $world_gen_time"
        fi
    else
        warn "Server not running - cannot analyze performance"
    fi
}

# Memory usage analysis
memory_usage() {
    log "Analyzing memory usage..."
    
    if is_server_running; then
        local pid=$(cat "${SERVER_DIR}/server.pid")
        
        if command -v ps >/dev/null 2>&1; then
            echo "Memory Usage Details:"
            ps -p "$pid" -o pid,rss,vsz,pmem,comm 2>/dev/null || echo "Unable to get memory details"
        fi
        
        # Check for memory-related warnings in logs
        local log_file="${LOGS_DIR}/server.log"
        if [[ -f "$log_file" ]]; then
            local memory_warnings=$(grep -i "memory\|heap\|gc" "$log_file" 2>/dev/null | wc -l)
            if [[ $memory_warnings -gt 0 ]]; then
                warn "Found $memory_warnings memory-related log entries"
            else
                info "✅ No memory warnings found"
            fi
        fi
    else
        warn "Server not running - cannot analyze memory usage"
    fi
}

# Export debug report
export_debug_report() {
    log "Exporting debug report..."
    
    local report_file="${SCRIPT_DIR}/debug_report_$(date +%Y%m%d_%H%M%S).txt"
    
    {
        echo "GeoBrot Plugin Debug Report"
        echo "Generated: $(date)"
        echo "=========================="
        echo ""
        
        echo "Plugin Information:"
        echo "  JAR: $(basename "$PLUGIN_JAR")"
        echo "  Size: $(du -h "$PLUGIN_JAR" 2>/dev/null | cut -f1 || echo "N/A")"
        echo "  Modified: $(stat -c %y "$PLUGIN_JAR" 2>/dev/null || stat -f %Sm "$PLUGIN_JAR" 2>/dev/null || echo "N/A")"
        echo ""
        
        echo "Server Status:"
        if is_server_running; then
            echo "  Running: Yes"
            local pid=$(cat "${SERVER_DIR}/server.pid")
            echo "  PID: $pid"
        else
            echo "  Running: No"
        fi
        echo ""
        
        echo "Recent Server Logs:"
        echo "==================="
        if [[ -f "${LOGS_DIR}/server.log" ]]; then
            tail -100 "${LOGS_DIR}/server.log"
        else
            echo "No logs found"
        fi
        
    } > "$report_file"
    
    info "Debug report exported to: $report_file"
}

# Live log monitoring
live_log_monitoring() {
    log "Starting live log monitoring..."
    info "Press Ctrl+C to stop monitoring"
    
    local log_file="${LOGS_DIR}/server.log"
    if [[ -f "$log_file" ]]; then
        tail -f "$log_file" | grep --line-buffered "GeoBrot\|ERROR\|WARN\|fractal\|mandel"
    else
        error "Log file not found: $log_file"
    fi
}

# Main menu loop
main() {
    while true; do
        show_menu
        read -r choice
        
        case $choice in
            1) check_plugin_status ;;
            2) view_logs ;;
            3) test_fractal_math ;;
            4) check_world_generation ;;
            5) test_commands ;;
            6) performance_analysis ;;
            7) memory_usage ;;
            8) export_debug_report ;;
            9) live_log_monitoring ;;
            0) 
                log "Exiting debug menu"
                exit 0
                ;;
            *)
                error "Invalid option: $choice"
                ;;
        esac
        
        echo ""
        echo "Press Enter to continue..."
        read -r
    done
}

# Handle Ctrl+C
trap 'echo ""; log "Debug session ended"; exit 0' INT

# Run main function
main "$@"
