package org.xpfarm.geobrot.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.xpfarm.geobrot.GeoBrotPlugin;
import org.xpfarm.geobrot.worlds.FractalWorldManager;
import org.xpfarm.geobrot.worlds.MandelbrotGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command executor for the /mandel command
 * 
 * Handles all fractal world management commands including creation,
 * teleportation, and world listing.
 */
public class MandelCommand implements CommandExecutor, TabCompleter {
    
    private final GeoBrotPlugin plugin;
    private final FractalWorldManager worldManager;
    
    /**
     * Create a new mandel command executor
     * 
     * @param plugin Plugin instance
     */
    public MandelCommand(GeoBrotPlugin plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleHelp(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "help" -> handleHelp(sender);
            case "create" -> handleCreate(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "list" -> handleList(sender);
            case "regen", "regenerate" -> handleRegenerate(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> {
                sender.sendMessage(Component.text("Unknown command. Use /mandel help for usage.", NamedTextColor.RED));
                yield true;
            }
        };
    }
    
    /**
     * Handle the help command
     */
    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== GeoBrot Fractal World Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/mandel help", NamedTextColor.YELLOW)
            .append(Component.text(" - Show this help message", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mandel create <name> [seed]", NamedTextColor.YELLOW)
            .append(Component.text(" - Create a new fractal world", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mandel tp <name>", NamedTextColor.YELLOW)
            .append(Component.text(" - Teleport to a fractal world", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mandel list", NamedTextColor.YELLOW)
            .append(Component.text(" - List all fractal worlds", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mandel regen <name>", NamedTextColor.YELLOW)
            .append(Component.text(" - Regenerate a fractal world", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mandel info <name>", NamedTextColor.YELLOW)
            .append(Component.text(" - Show fractal world information", NamedTextColor.GRAY)));
        
        return true;
    }
    
    /**
     * Handle the create command
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geobrot.create")) {
            sender.sendMessage(Component.text("You don't have permission to create fractal worlds!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mandel create <name> [seed]", NamedTextColor.RED));
            return true;
        }
        
        String worldName = args[1];
        String seed = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
        
        // Check if world already exists
        if (worldManager.isFractalWorld(worldName)) {
            sender.sendMessage(Component.text("Fractal world '" + worldName + "' already exists!", NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("Creating fractal world '" + worldName + "'...", NamedTextColor.YELLOW));
        
        // Create world asynchronously to avoid blocking
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            World world = worldManager.createFractalWorld(worldName, seed);
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (world != null) {
                    sender.sendMessage(Component.text("Successfully created fractal world: ", NamedTextColor.GREEN)
                        .append(Component.text(worldName, NamedTextColor.GOLD)));
                    
                    if (seed != null) {
                        sender.sendMessage(Component.text("Seed: ", NamedTextColor.GRAY)
                            .append(Component.text(seed, NamedTextColor.WHITE)));
                    }
                } else {
                    sender.sendMessage(Component.text("Failed to create fractal world '" + worldName + "'!", NamedTextColor.RED));
                }
            });
        });
        
        return true;
    }
    
    /**
     * Handle the teleport command
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can teleport to fractal worlds!", NamedTextColor.RED));
            return true;
        }
        
        if (!sender.hasPermission("geobrot.teleport")) {
            sender.sendMessage(Component.text("You don't have permission to teleport to fractal worlds!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mandel tp <name>", NamedTextColor.RED));
            return true;
        }
        
        String worldName = args[1];
        World world = worldManager.getFractalWorld(worldName);
        
        if (world == null) {
            sender.sendMessage(Component.text("Fractal world '" + worldName + "' not found!", NamedTextColor.RED));
            return true;
        }
        
        // Find a safe spawn location by searching for solid blocks
        Location spawnLocation = findSafeSpawnLocation(world);
        
        if (spawnLocation == null) {
            sender.sendMessage(Component.text("Could not find a safe spawn location in fractal world '" + worldName + "'!", NamedTextColor.RED));
            return true;
        }
        
        player.teleport(spawnLocation);
        sender.sendMessage(Component.text("Teleported to fractal world: ", NamedTextColor.GREEN)
            .append(Component.text(worldName, NamedTextColor.GOLD)));
        
        return true;
    }
    
    /**
     * Find a safe spawn location in the fractal world
     * 
     * @param world The fractal world
     * @return Safe spawn location or null if none found
     */
    private Location findSafeSpawnLocation(World world) {
        // Get the world generator to find a proper spawn location
        MandelbrotGenerator generator = worldManager.getWorldGenerator(world.getName());
        if (generator != null) {
            return generator.findSpawnLocation(world);
        }
        
        // Fallback method if generator not available
        // Search in a spiral pattern from the center
        int centerX = 0;
        int centerZ = 0;
        int maxRadius = 100;
        
        // Start searching from the center outwards
        for (int radius = 0; radius < maxRadius; radius += 16) {
            for (int x = centerX - radius; x <= centerX + radius; x += 8) {
                for (int z = centerZ - radius; z <= centerZ + radius; z += 8) {
                    Location safeLocation = findSafeLocationAt(world, x, z);
                    if (safeLocation != null) {
                        return safeLocation;
                    }
                }
            }
        }
        
        // If no safe location found, return a location above the world center
        return new Location(world, centerX + 0.5, 100, centerZ + 0.5);
    }
    
    /**
     * Check if a location is safe for player spawning
     * 
     * @param world The world to check in
     * @param x The X coordinate to check
     * @param z The Z coordinate to check
     * @return Safe location or null if not safe
     */
    private Location findSafeLocationAt(World world, int x, int z) {
        // Search from top to bottom for a safe spawn
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            
            // Check if there's a solid block at this location
            if (loc.getBlock().getType().isSolid()) {
                // Check if there's enough space above for the player
                Location above1 = loc.clone().add(0, 1, 0);
                Location above2 = loc.clone().add(0, 2, 0);
                
                if (above1.getBlock().getType().isAir() && above2.getBlock().getType().isAir()) {
                    // Safe location found - spawn the player one block above the solid block
                    return above1;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Handle the list command
     */
    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("geobrot.list")) {
            sender.sendMessage(Component.text("You don't have permission to list fractal worlds!", NamedTextColor.RED));
            return true;
        }
        
        var worldNames = worldManager.getFractalWorldNames();
        
        if (worldNames.isEmpty()) {
            sender.sendMessage(Component.text("No fractal worlds found.", NamedTextColor.YELLOW));
            return true;
        }
        
        sender.sendMessage(Component.text("=== Fractal Worlds ===", NamedTextColor.GOLD));
        for (String worldName : worldNames) {
            World world = worldManager.getFractalWorld(worldName);
            String status = world != null ? "Loaded" : "Unloaded";
            NamedTextColor statusColor = world != null ? NamedTextColor.GREEN : NamedTextColor.RED;
            
            sender.sendMessage(Component.text("• " + worldName + " - ", NamedTextColor.YELLOW)
                .append(Component.text(status, statusColor)));
        }
        
        return true;
    }
    
    /**
     * Handle the regenerate command
     */
    private boolean handleRegenerate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("geobrot.regenerate")) {
            sender.sendMessage(Component.text("You don't have permission to regenerate fractal worlds!", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mandel regen <name>", NamedTextColor.RED));
            return true;
        }
        
        String worldName = args[1];
        
        if (!worldManager.isFractalWorld(worldName)) {
            sender.sendMessage(Component.text("Fractal world '" + worldName + "' not found!", NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("Regenerating fractal world '" + worldName + "'...", NamedTextColor.YELLOW));
        
        // Regenerate world asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = worldManager.regenerateFractalWorld(worldName);
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(Component.text("Successfully regenerated fractal world: ", NamedTextColor.GREEN)
                        .append(Component.text(worldName, NamedTextColor.GOLD)));
                } else {
                    sender.sendMessage(Component.text("Failed to regenerate fractal world '" + worldName + "'!", NamedTextColor.RED));
                }
            });
        });
        
        return true;
    }
    
    /**
     * Handle the info command
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mandel info <name>", NamedTextColor.RED));
            return true;
        }
        
        String worldName = args[1];
        double[] params = worldManager.getWorldParameters(worldName);
        
        if (params == null) {
            sender.sendMessage(Component.text("Fractal world '" + worldName + "' not found!", NamedTextColor.RED));
            return true;
        }
        
        sender.sendMessage(Component.text("=== Fractal World Info: " + worldName + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Center X: ", NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.6f", params[0]), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Center Y: ", NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.6f", params[1]), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Zoom: ", NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.3f", params[2]), NamedTextColor.WHITE)));
        
        World world = worldManager.getFractalWorld(worldName);
        if (world != null) {
            sender.sendMessage(Component.text("Players: ", NamedTextColor.YELLOW)
                .append(Component.text(world.getPlayers().size() + "", NamedTextColor.WHITE)));
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("help", "create", "tp", "list", "regen", "info");
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("tp".equals(subCommand) || "teleport".equals(subCommand) || 
                "regen".equals(subCommand) || "regenerate".equals(subCommand) || 
                "info".equals(subCommand)) {
                // Second argument - world names
                return worldManager.getFractalWorldNames().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
