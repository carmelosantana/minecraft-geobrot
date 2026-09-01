# Contributing to GeoBrot Plugin

- [Environment](#environment)
  - [Version Compatibility](#version-compatibility)
- [Testing](#testing)
- [Building the Plugin](#building-the-plugin)
- [Author](#author)
- [Server Management](#server-management)
- [Docker](#docker)
  - [Linux Firewall](#linux-firewall)
- [Console Output Guidelines](#console-output-guidelines)
- [Writing Style](#writing-style)

This file provides guidelines for contributing to our GeoBrot Plugin development, including setting up the development environment, building the plugin, testing, and server management. It also includes Docker setup instructions and author information. Please follow these guidelines to ensure a smooth development process and consistent coding practices.

## Environment

- Java 25
- Minecraft Java Edition 26.1+
- Paper 26.1.2+ (recommended)
- ViaVersion plugin (for Geyser compatibility)

### Version Compatibility

- **Geyser/Floodgate**: Requires Minecraft 26.1+ or ViaVersion for compatibility
- **ViaVersion**: Automatically downloaded and installed for better cross-version support
- **Paper**: Latest 26.1.2 builds recommended for optimal performance

## Testing

- Always test shell scripts with `bash -n` to check syntax.
- Use `shellcheck` to catch common shell script issues.
- For Java code, run `mvn test` to execute unit tests.
- Always create in game commands for testing, debugging, and configuration changes.

## Building the Plugin

- Always provide a `Makefile` for easy build commands.
- Always create a JAR file in the `target/` directory after running `mvn clean package`.
- Ensure the plugin is compatible with Minecraft Java Edition 26.1+.
- Use `NamespacedKey` for custom recipes to avoid conflicts.
- Use cached `ItemStack` instances for performance.
- Use event driven architecture to minimize performance overhead.
- Always use a config file for customizable options like fractal parameters and materials.
- Always include a README with clear instructions for building, installing, and configuring the plugin. Do not create a new README for each change, always update the existing README.

## Author

- Carmelo Santana ([@carmelosantana](https://github.com/carmelosantana))
- Plugin or Author URL `xp.farm`
- Use namespace `org.xpfarm.*` for all plugin classes
- Do not include author information in the code comments or documentation, only in POM files or metadata where appropriate.

## Server Management

- Use a `Makefile` for server management commands.
  - Provide commands for;
    - `make setup` to prepare the server environment.
    - `make start` to start the server.
    - `make stop` to stop the server.
    - `make restart` to restart the server.
    - `make status` to check server status.
    - `make logs` to view server logs.
    - `make clean` to clean up temporary files.
    - `make test` to run tests.
    - `make network` to check network connectivity and display server IP.
    - `make attach` to attach to the server console.
    - `make players` to list online players.
- Provide a `server-manager.sh` script for direct server management.
- Ensure the server is configured to accept EULA and has the correct Java version.

## Docker

- Always provide a `docker-compose.yml` file for easy container setup.
- Ensure the Docker container is configured to run the plugin with the correct Java version.
- Use `docker-test.sh` to validate the Docker setup and ensure the plugin runs correctly in a container.
- Configure ViaVersion installation for Geyser compatibility.

```yml
# Minecraft Java Paper Server + Geyser + Floodgate Docker Container
# https://github.com/TheRemote/Legendary-Java-Minecraft-Geyser-Floodgate
services:
  geobrot:
    image: 05jchambers/legendary-minecraft-geyser-floodgate:latest
    restart: "unless-stopped"
    ports:
      - 25565:25565
      - 19132:19132
      - 19132:19132/udp
    volumes:
      - minecraft:/minecraft
    stdin_open: true # docker run -i
    tty: true # docker run -t
    entrypoint: ["/bin/bash", "/scripts/start.sh"]
    environment:
      Port: "25565"
      BedrockPort: "19132"
      TZ: "America/New_York"
      MaxMemory: 4096
      Version: "26.1.2" # Force latest Paper version
      NoBackup: "plugins" # Optional folder to skip during backups
      NoPermCheck: "Y" # Optional flag to skip permissions check
      # NoViaVersion: "Y" # Enable ViaVersion for Geyser compatibility
      QuietCurl: "Y" # Optional flag to reduce curl log output
volumes:
  minecraft:
    driver: local
```

### Linux Firewall

```bash
# Allow Minecraft server port (default 25565)
sudo ufw allow 25565/tcp
# Allow Bedrock server port (default 19132) both TCP and UDP
sudo ufw allow 19132/tcp
sudo ufw allow 19132/udp
```

## Console Output Guidelines

All console messages must use consistent color and formatting to ensure a unified user experience across plugins:

- Use `Component.text()` with `NamedTextColor` instead of legacy `ChatColor`
- **Titles**: `NamedTextColor.GOLD` for section headers and plugin names
- **Commands**: `NamedTextColor.YELLOW` for command syntax
- **Descriptions**: `NamedTextColor.GRAY` for help text and descriptions
- **Lists**: `NamedTextColor.AQUA` for list labels, `NamedTextColor.WHITE` for list items
- **Success**: `NamedTextColor.GREEN` for successful operations
- **Errors**: `NamedTextColor.RED` for error messages

Example:

```java
sender.sendMessage(Component.text("=== GeoBrot Commands ===", NamedTextColor.GOLD));
sender.sendMessage(Component.text("/mandel help", NamedTextColor.YELLOW)
    .append(Component.text(" - Show help message", NamedTextColor.GRAY)));
sender.sendMessage(Component.text("Available worlds: ", NamedTextColor.AQUA)
    .append(Component.text("world1, world2", NamedTextColor.WHITE)));
```

## Development Environment

### Initial Setup

All plugins use a consistent development workflow with Make-based automation;

```bash
# Clone any plugin repository
git clone <plugin-repository>
cd <plugin-directory>

# Set up development environment
make setup

# Build the plugin
make build

# Start development server
make start
```

### Development Workflow

These plugins follow a standardized development cycle;

```bash
# Quick development cycle (build + install + restart)
make dev

# View server logs
make logs

# Check server status
make status

# Stop the server
make stop

# Clean up for fresh start
make clean
```

### Available Make Targets

Every plugin supports these standard Make targets;

| Target | Description |
|--------|-------------|
| `make help` | Show all available commands |
| `make setup` | Initial server setup and dependencies |
| `make build` | Build the plugin JAR |
| `make start` | Start the test server |
| `make stop` | Stop the test server |
| `make restart` | Restart the server |
| `make dev` | Quick development cycle (build + install + restart) |
| `make test` | Run unit tests |
| `make docker-test` | Test in Docker container |
| `make clean` | Clean server files |
| `make logs` | Show server logs |
| `make status` | Check server status |
| `make debug` | Interactive debug menu |

## Code Standards

### Java Coding Standards

- **Java Version**: Target Java 25+ features
- **Code Style**: ~~Follow standard Java conventions~~ Mostly output from Claude and ChatGPT
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: ~Maximum 120 characters
- **Comments**: Use JavaDoc for public methods and classes

### Plugin Architecture

- **Event Driven**: Use Bukkit event system efficiently
- **Asynchronous Processing**: Network operations and heavy tasks should be async
- **Memory Efficient**: Implement proper cleanup and resource management
- **Error Handling**: Graceful degradation and comprehensive error handling

### Configuration Standards

- **YAML Format**: All configuration files use YAML
- **Validation**: Validate configuration on startup
- **Defaults**: Provide sensible defaults for all settings
- **Documentation**: Comment configuration options clearly

### Example Code Structure

```java
package org.xpfarm.geobrot;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for GeoBrot
 */
public class GeoBrotPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Plugin initialization
        saveDefaultConfig();
        registerCommands();
        registerEvents();
        
        getLogger().info("GeoBrot plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cleanup
        getLogger().info("GeoBrot plugin disabled!");
    }
}
```

## Server Management

### Server Manager Script

All plugins include a comprehensive server management script:

```bash
# Direct script usage
./server-manager.sh setup     # Initial setup
./server-manager.sh start     # Start server
./server-manager.sh stop      # Stop server
./server-manager.sh restart   # Restart server
./server-manager.sh attach    # Attach to server console
./server-manager.sh players   # Show online players
./server-manager.sh backup    # Create backup
./server-manager.sh restore   # Restore from backup
```

### Configuration Management

- **EULA**: Automatically accepted during setup
- **Server Properties**: Optimized for development
- **Plugin Configs**: Default configurations provided
- **World Generation**: Consistent world settings

## Testing Strategy

### Unit Testing

```bash
# Run all tests
make test

# Run specific test class
mvn test -Dtest=FractalMathTest

# Run with coverage
mvn test jacoco:report
```

### Integration Testing

```bash
# Test in local server
make dev

# Test in Docker container
make docker-test

# Manual testing commands
/mandel create testworld
/mandel tp testworld
```

### Performance Testing

```bash
# Monitor generation performance
make debug
# Select option 6: Performance Analysis

# Check memory usage
make debug
# Select option 7: Memory Usage
```

## Docker Development

### Container Testing

```bash
# Build and start container
make docker-test

# View container logs
docker-compose logs geobrot

# Stop container
docker-compose down
```

### Volume Management

- Plugin JAR is mounted as volume for hot reloading
- World data persists between container restarts
- Configuration files can be edited externally

## Troubleshooting

### Common Issues

1. **Build Failures**: Check Java version and Maven installation
2. **Server Won't Start**: Verify EULA acceptance and port availability
3. **Plugin Loading Issues**: Check plugin.yml syntax and dependencies
4. **Permission Problems**: Ensure correct file permissions on scripts

### Debug Tools

- Use `make debug` for interactive debugging
- Check server logs with `make logs`
- Monitor performance with built-in tools
- Use Docker for isolated testing

## Release Process

1. **Update Version**: Bump version in pom.xml and plugin.yml
2. **Run Tests**: Execute `make test` and `make docker-test`
3. **Build Release**: Run `make release`
4. **Tag Release**: Create git tag with version number
5. **Documentation**: Update README with new features

## Writing Style

When writing documentation and comments avoid any sentence structures that set up and then negate or expand beyond expectations (like 'X isn't just about Y' or 'X is more than just Y'). Instead, use direct, affirmative statements. Feel free to be creative with your sentence structures and expression styles.
