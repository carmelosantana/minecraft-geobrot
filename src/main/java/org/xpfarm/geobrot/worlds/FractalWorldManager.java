package org.xpfarm.geobrot.worlds;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.xpfarm.geobrot.GeoBrotPlugin;
import org.xpfarm.geobrot.utils.FractalMath;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages fractal worlds in the plugin
 * 
 * Handles creation, loading, and management of Mandelbrot-based worlds,
 * including persistence of world metadata.
 */
public class FractalWorldManager {
    
    private final GeoBrotPlugin plugin;
    private final Map<String, World> fractalWorlds;
    private final Map<String, double[]> worldParameters;
    private final File worldsFile;
    private FileConfiguration worldsConfig;
    private final TerrainProfile terrainProfile;
    private final GeodePalette geodePalette;

    /**
     * Create a new fractal world manager
     *
     * @param plugin Plugin instance
     */
    public FractalWorldManager(GeoBrotPlugin plugin) {
        this.plugin = plugin;
        this.fractalWorlds = new HashMap<>();
        this.worldParameters = new HashMap<>();
        this.worldsFile = new File(plugin.getDataFolder(), "fractal_worlds.yml");
        this.terrainProfile = TerrainProfile.fromConfig(plugin.getConfig());
        this.geodePalette = GeodePalette.fromConfig(plugin.getConfig());

        loadWorldsConfig();
        loadExistingWorlds();
    }
    
    /**
     * Resolve a {@code /mandel create} argument to fractal parameters.
     *
     * <p>The argument is matched case-insensitively against the preset names under
     * {@code defaults.presets} in {@code config}; a match reads that preset's
     * {@code center-x}, {@code center-y}, and {@code zoom}. A {@code null} argument or an
     * argument that doesn't name a known preset falls back to
     * {@link FractalMath#seedToFractalParams(String)} (the seed-hashing behavior), so an
     * arbitrary seed string still works exactly as before.
     *
     * @param arg the create-command argument: {@code null}, a preset name, or a seed
     * @param config the plugin configuration holding {@code defaults.presets}
     * @return array with {@code [centerX, centerY, zoom]}
     */
    static double[] resolveFractalParams(String arg, FileConfiguration config) {
        if (arg == null) {
            return FractalMath.seedToFractalParams(null);
        }

        ConfigurationSection presets = config.getConfigurationSection("defaults.presets");
        if (presets != null) {
            for (String presetName : presets.getKeys(false)) {
                if (presetName.equalsIgnoreCase(arg)) {
                    double centerX = presets.getDouble(presetName + ".center-x");
                    double centerY = presets.getDouble(presetName + ".center-y");
                    double zoom = presets.getDouble(presetName + ".zoom");
                    return new double[]{centerX, centerY, zoom};
                }
            }
        }

        return FractalMath.seedToFractalParams(arg);
    }

    /**
     * Get the preset names available under {@code defaults.presets}, for tab-completion.
     *
     * @return set of configured preset names, or an empty set if none are configured
     */
    public Set<String> getPresetNames() {
        ConfigurationSection presets = plugin.getConfig().getConfigurationSection("defaults.presets");
        if (presets == null) {
            return Set.of();
        }
        return presets.getKeys(false);
    }

    /**
     * Create a new fractal world
     *
     * @param name World name
     * @param seed Seed string (optional)
     * @return Created world or null if failed
     */
    public World createFractalWorld(String name, String seed) {
        if (fractalWorlds.containsKey(name)) {
            return null; // World already exists
        }

        // Resolve fractal parameters: a known preset name, or fall back to seed hashing
        double[] params = resolveFractalParams(seed, plugin.getConfig());
        double centerX = params[0];
        double centerY = params[1];
        double zoom = params[2];
        
        // Create the world
        MandelbrotGenerator generator = new MandelbrotGenerator(centerX, centerY, zoom, terrainProfile, geodePalette);
        WorldCreator creator = new WorldCreator(name);
        
        // Configure world settings for proper generation
        creator.generator(generator);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT); // Use flat world type for custom generation
        creator.generateStructures(false);
        creator.seed(seed != null ? seed.hashCode() : System.currentTimeMillis());
        
        try {
            World world = creator.createWorld();
            if (world != null) {
                // Configure world spawn settings
                configureWorldSettings(world);
                
                fractalWorlds.put(name, world);
                worldParameters.put(name, params);
                
                // Save world metadata
                saveWorldMetadata(name, seed, params);
                
                plugin.getLogger().info("Created fractal world: " + name);
                return world;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create fractal world '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Configure world settings after creation
     * 
     * @param world The world to configure
     */
    private void configureWorldSettings(World world) {
        // Don't set spawn location immediately - wait for terrain generation
        // The spawn will be set when players first teleport
        
        // Configure game rules for fractal worlds
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        
        // Set world time to day
        world.setTime(1000L);
        
        plugin.getLogger().info("Configured settings for fractal world: " + world.getName());
    }
    
    /**
     * Get the generator for a fractal world
     * 
     * @param worldName The world name
     * @return The MandelbrotGenerator or null if not found
     */
    public MandelbrotGenerator getWorldGenerator(String worldName) {
        double[] params = worldParameters.get(worldName);
        if (params != null) {
            return new MandelbrotGenerator(params[0], params[1], params[2], terrainProfile, geodePalette);
        }
        return null;
    }
    
    /**
     * Get a fractal world by name
     * 
     * @param name World name
     * @return World or null if not found
     */
    public World getFractalWorld(String name) {
        return fractalWorlds.get(name);
    }
    
    /**
     * Get all fractal world names
     * 
     * @return Set of world names
     */
    public Set<String> getFractalWorldNames() {
        return fractalWorlds.keySet();
    }
    
    /**
     * Check if a world is a fractal world
     * 
     * @param worldName World name
     * @return True if it's a fractal world
     */
    public boolean isFractalWorld(String worldName) {
        return fractalWorlds.containsKey(worldName);
    }
    
    /**
     * Get world parameters
     * 
     * @param worldName World name
     * @return Array with [centerX, centerY, zoom] or null if not found
     */
    public double[] getWorldParameters(String worldName) {
        return worldParameters.get(worldName);
    }
    
    /**
     * Regenerate a fractal world
     * 
     * @param name World name
     * @return True if regenerated successfully
     */
    public boolean regenerateFractalWorld(String name) {
        World world = fractalWorlds.get(name);
        if (world == null) {
            return false;
        }
        
        try {
            // Unload the world
            Bukkit.unloadWorld(world, false);
            
            // Delete world files
            File worldFolder = new File(Bukkit.getWorldContainer(), name);
            if (worldFolder.exists()) {
                deleteWorldFolder(worldFolder);
            }
            
            // Recreate the world
            double[] params = worldParameters.get(name);
            if (params != null) {
                MandelbrotGenerator generator = new MandelbrotGenerator(params[0], params[1], params[2], terrainProfile, geodePalette);
                WorldCreator creator = new WorldCreator(name);
                creator.generator(generator);
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                
                World newWorld = creator.createWorld();
                if (newWorld != null) {
                    // Configure world settings
                    configureWorldSettings(newWorld);
                    
                    fractalWorlds.put(name, newWorld);
                    plugin.getLogger().info("Regenerated fractal world: " + name);
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to regenerate fractal world '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Load worlds configuration
     */
    private void loadWorldsConfig() {
        if (!worldsFile.exists()) {
            try {
                worldsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create worlds config file: " + e.getMessage());
            }
        }
        
        worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
    }
    
    /**
     * Load existing fractal worlds
     */
    private void loadExistingWorlds() {
        if (worldsConfig.contains("worlds")) {
            for (String worldName : worldsConfig.getConfigurationSection("worlds").getKeys(false)) {
                String path = "worlds." + worldName;
                
                double centerX = worldsConfig.getDouble(path + ".centerX", -0.7);
                double centerY = worldsConfig.getDouble(path + ".centerY", 0.0);
                double zoom = worldsConfig.getDouble(path + ".zoom", 1.0);
                
                worldParameters.put(worldName, new double[]{centerX, centerY, zoom});
                
                // Try to load the world if it exists
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    fractalWorlds.put(worldName, world);
                    plugin.getLogger().info("Loaded existing fractal world: " + worldName);
                }
            }
        }
    }
    
    /**
     * Save world metadata
     * 
     * @param name World name
     * @param seed Original seed
     * @param params Fractal parameters
     */
    private void saveWorldMetadata(String name, String seed, double[] params) {
        String path = "worlds." + name;
        
        worldsConfig.set(path + ".seed", seed);
        worldsConfig.set(path + ".centerX", params[0]);
        worldsConfig.set(path + ".centerY", params[1]);
        worldsConfig.set(path + ".zoom", params[2]);
        worldsConfig.set(path + ".created", System.currentTimeMillis());
        
        try {
            worldsConfig.save(worldsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save world metadata: " + e.getMessage());
        }
    }
    
    /**
     * Delete a world folder recursively
     * 
     * @param folder Folder to delete
     */
    private void deleteWorldFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorldFolder(file);
                }
            }
        }
        folder.delete();
    }
    
    /**
     * Shutdown the world manager
     */
    public void shutdown() {
        // Save any pending changes
        try {
            worldsConfig.save(worldsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save worlds config on shutdown: " + e.getMessage());
        }
        
        fractalWorlds.clear();
        worldParameters.clear();
    }
}
