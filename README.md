# 🌀 GeoBrot Plugin

> Generate and explore floating island worlds shaped like the Mandelbrot and Buddhabrot fractals. Create stunning 2D mathematical landscapes that resemble geodes, with built-in world creation, teleportation, and visualization.

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1-green.svg)](https://minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-26.1-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

## ✨ Features

### 🎯 Core Functionality
- **Fractal World Generation**: Create floating islands based on the Mandelbrot set
- **Multiple Fractal Types**: Support for different fractal patterns and zoom levels
- **Seed-Based Generation**: Use custom seeds to generate unique fractal worlds
- **Geode-like Structures**: Beautiful layered materials that resemble natural geodes
- **Teleportation System**: Easy world navigation with `/mandel tp`
- **World Management**: Create, regenerate, and manage multiple fractal worlds

### 🎨 Visual Design
- **Layered Materials**: Different materials based on fractal escape-time values
- **Amethyst Geodes**: Deep fractal areas feature amethyst and calcite
- **Prismarine Structures**: Medium-depth areas with prismarine variants
- **Copper Formations**: Copper blocks for transitional areas
- **Stone Foundations**: Classic stone materials for shallow areas

### 🔧 Technical Features
- **Asynchronous Generation**: Non-blocking world creation
- **Persistent Worlds**: Automatic saving and loading of fractal worlds
- **Configuration System**: Customizable materials, sizes, and parameters
- **Performance Optimized**: Efficient fractal calculations and chunk generation

## 🛠 Installation

### Requirements

- **Java**: 21 or higher
- **Server**: Paper 1.21.6+, Spigot 1.21+, or Bukkit 1.21+
- **Maven**: For building from source

### Quick Install

1. Download the latest JAR from releases
2. Place in your server's `plugins/` directory
3. Restart your server
4. Configure in `plugins/GeoBrot/config.yml` if needed

### Build from Source

```bash
# Clone the repository
git clone https://github.com/carmelosantana/geobrot
cd geobrot

# Build the plugin
make build

# Install to test server
make install
```

## 🎮 Usage

### Commands

All commands use the `/mandel` base command:

- `/mandel help` - Show command help
- `/mandel create <name> [seed]` - Create a new fractal world
- `/mandel tp <name>` - Teleport to a fractal world
- `/mandel list` - List all fractal worlds
- `/mandel info <name>` - Show fractal world information
- `/mandel regen <name>` - Regenerate a fractal world

### Examples

```bash
# Create a world with a specific seed
/mandel create myworld spiral

# Create a world with random generation
/mandel create testworld

# Teleport to a fractal world
/mandel tp myworld

# List all fractal worlds
/mandel list

# Show world information
/mandel info myworld

# Regenerate a world
/mandel regen myworld
```

### Predefined Seeds

The plugin includes several interesting fractal presets:

- **classic**: The classic Mandelbrot set view
- **spiral**: Beautiful spiral patterns
- **seahorse**: Seahorse-like fractal formations
- **elephant**: Elephant-shaped fractal areas

## 🔧 Configuration

### Basic Settings

```yaml
# config.yml
generation:
  default-world-size: 512
  base-height: 64
  max-thickness: 16
  default-zoom: 1.0

materials:
  deep:
    core: CALCITE
    middle: AMETHYST_BLOCK
    surface: BUDDING_AMETHYST
```

### Advanced Configuration

The plugin supports extensive customization:

- **Material Palettes**: Define custom material combinations
- **Fractal Parameters**: Adjust zoom levels and center points
- **Performance Settings**: Control generation speed and memory usage
- **World Limits**: Set maximum number of fractal worlds

## 🔧 Development

### Development Setup

```bash
# Check dependencies
make check-deps

# Set up development environment
make setup

# Quick development cycle
make dev    # build + install + restart
```

### Testing

```bash
# Run unit tests
make test

# Test in Docker
make docker-test

# Interactive debugging
make debug
```

### Server Management

```bash
# Start test server
make start

# View server logs
make logs

# Show server status
make status

# Clean restart
make reset
```

## 🐳 Docker Support

Test the plugin easily with Docker:

```bash
# Build and test in container
make docker-test

# Or manually
docker-compose up -d
```

Server will be available at:

- **Java Edition**: `localhost:25565`
- **Bedrock Edition**: `localhost:19132`

## 📋 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `geobrot.use` | Basic plugin usage | `true` |
| `geobrot.create` | Create fractal worlds | `op` |
| `geobrot.teleport` | Teleport to fractal worlds | `true` |
| `geobrot.list` | List fractal worlds | `true` |
| `geobrot.regenerate` | Regenerate fractal worlds | `op` |

## 🔬 Technical Details

### Fractal Mathematics

The plugin uses the Mandelbrot set escape-time algorithm to generate terrain:

```java
// Basic Mandelbrot calculation
int escapeTime = mandelbrotEscapeTime(x, y);
if (escapeTime > threshold) {
    generateTerrain(x, y, escapeTime);
}
```

### World Generation

1. **Coordinate Mapping**: World coordinates are mapped to fractal coordinates
2. **Escape-Time Calculation**: Each point's escape time is calculated
3. **Terrain Generation**: Land is generated based on escape time thresholds
4. **Material Selection**: Materials are chosen based on escape time depth

### Performance Considerations

- **Cached Calculations**: Fractal values are cached where possible
- **Asynchronous Processing**: World generation doesn't block the main thread
- **Optimized Algorithms**: Efficient escape-time calculations
- **Memory Management**: Proper cleanup of world data

## 🐛 Troubleshooting

### Common Issues

**Plugin not loading:**
- Check Java version (requires 21+)
- Verify Paper/Spigot version (1.21+)
- Check server logs for errors

**World generation failing:**
- Ensure sufficient memory allocation
- Check for conflicting world generators
- Verify plugin permissions

**Performance issues:**
- Reduce world size in config
- Increase server memory allocation
- Limit concurrent world operations

### Debug Mode

Enable debug logging in config.yml:

```yaml
debug:
  enabled: true
  log-calculations: true
  log-timing: true
```

Use the debug script for interactive troubleshooting:

```bash
make debug
```

## 📊 Performance

### Benchmarks

| Operation | Time | Memory |
|-----------|------|--------|
| Create 512x512 world | ~30s | ~512MB |
| Generate single chunk | ~50ms | ~16MB |
| Teleport to world | ~100ms | ~8MB |

### Optimization Tips

1. **Reduce World Size**: Smaller worlds generate faster
2. **Increase RAM**: More memory allows for better caching
3. **Limit Concurrent Worlds**: Don't generate multiple worlds simultaneously
4. **Use SSD Storage**: Faster disk I/O improves performance

## 🤝 Contributing

Please review [Contributing Guidelines](./CONTRIBUTING.md) for developer documentation, including:

- Development environment setup
- Code standards and testing procedures
- Pull request process
- Server management and Docker support
- Troubleshooting

### Developer Quick Start

```bash
# Set up development environment
make setup

# Quick development cycle
make dev

# Run tests
make test

# Test in Docker
make docker-test
```

## 📄 License

Licensed under the GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later). See the [LICENSE](./LICENSE) file for the full text.

## 👥 Credits

- **Author**: Carmelo Santana
- **Website**: https://xpfarm.org
- **Live Server**: play.xpfarm.org
- **Docker Container**: [Legendary Minecraft Geyser](https://github.com/TheRemote/Legendary-Java-Minecraft-Geyser-Floodgate)

## 🔗 Links

- [GitHub Repository](https://github.com/carmelosantana/geobrot)
- [Live Test Server](https://play.xpfarm.org)
- [Bug Reports](https://github.com/carmelosantana/geobrot/issues)

---

*Experience the beauty of mathematics in Minecraft with GeoBrot! 🌀*