package org.xpfarm.geobrot.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Configuration validator for GeoBrot plugin
 * 
 * Validates and provides defaults for plugin configuration values
 */
public class ConfigValidator {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private final FileConfiguration config;
    
    public ConfigValidator(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = plugin.getConfig();
    }

    /**
     * Test seam: validate an arbitrary {@link FileConfiguration} without requiring a running
     * server. There is no backing {@link JavaPlugin}, so {@link JavaPlugin#saveConfig()} is
     * never invoked - the caller owns persisting the (in-memory) configuration it passed in.
     */
    ConfigValidator(FileConfiguration config, Logger logger) {
        this.plugin = null;
        this.logger = logger;
        this.config = config;
    }

    /**
     * Validate and fix the configuration
     *
     * @return true if configuration is valid or was successfully fixed
     */
    public boolean validateConfig() {
        boolean isValid = true;

        // Validate generation settings
        if (!validateGenerationSettings()) {
            isValid = false;
        }

        // Validate material settings
        if (!validateMaterialSettings()) {
            isValid = false;
        }

        // Validate performance settings
        if (!validatePerformanceSettings()) {
            isValid = false;
        }

        // Validate permissions settings
        if (!validatePermissionSettings()) {
            isValid = false;
        }

        // Save configuration if changes were made
        if (!isValid) {
            if (plugin != null) {
                plugin.saveConfig();
            }
            logger.info("Configuration has been validated and updated");
        }

        return true; // Always return true as we fix issues
    }
    
    private boolean validateGenerationSettings() {
        boolean isValid = true;
        
        // Validate world size
        int worldSize = config.getInt("generation.world_size", 2000);
        if (worldSize < 500 || worldSize > 10000) {
            logger.warning("Invalid world_size: " + worldSize + ". Setting to default 2000");
            config.set("generation.world_size", 2000);
            isValid = false;
        }
        
        // Validate max iterations
        int maxIterations = config.getInt("generation.max_iterations", 100);
        if (maxIterations < 10 || maxIterations > 1000) {
            logger.warning("Invalid max_iterations: " + maxIterations + ". Setting to default 100");
            config.set("generation.max_iterations", 100);
            isValid = false;
        }
        
        // Validate zoom range
        double minZoom = config.getDouble("generation.zoom_range.min", 0.1);
        double maxZoom = config.getDouble("generation.zoom_range.max", 5.0);
        if (minZoom <= 0 || maxZoom <= minZoom) {
            logger.warning("Invalid zoom range: min=" + minZoom + ", max=" + maxZoom + ". Setting to defaults");
            config.set("generation.zoom_range.min", 0.1);
            config.set("generation.zoom_range.max", 5.0);
            isValid = false;
        }
        
        // Validate sea level
        int seaLevel = config.getInt("generation.sea_level", 60);
        if (seaLevel < 0 || seaLevel > 255) {
            logger.warning("Invalid sea_level: " + seaLevel + ". Setting to default 60");
            config.set("generation.sea_level", 60);
            isValid = false;
        }

        // Validate terrain floor Y (see TerrainProfile.fromConfig for the matching defaults)
        int floorY = config.getInt("generation.floor-y", 135);
        if (floorY < -64 || floorY > 300) {
            logger.warning("Invalid floor-y: " + floorY + ". Setting to default 135");
            config.set("generation.floor-y", 135);
            isValid = false;
        }

        // Validate terrain surface base Y
        int surfaceBaseY = config.getInt("generation.surface-base-y", 153);
        if (surfaceBaseY < -64 || surfaceBaseY > 300) {
            logger.warning("Invalid surface-base-y: " + surfaceBaseY + ". Setting to default 153");
            config.set("generation.surface-base-y", 153);
            isValid = false;
        }

        // Validate terrain relief amplitude
        int reliefAmplitude = config.getInt("generation.relief-amplitude", 12);
        if (reliefAmplitude < 0 || reliefAmplitude > 64) {
            logger.warning("Invalid relief-amplitude: " + reliefAmplitude + ". Setting to default 12");
            config.set("generation.relief-amplitude", 12);
            isValid = false;
        }

        return isValid;
    }

    /** The four escape-time material tiers, matching {@code materials.*} in config.yml. */
    private static final String[] MATERIAL_TIERS = {"deep", "medium-deep", "medium", "shallow"};

    private boolean validateMaterialSettings() {
        boolean isValid = true;

        // Verify every escape-time tier is present; if the whole section is missing (or any
        // tier within it is), seed the shipped config.yml defaults for all four tiers.
        boolean missingTier = !config.contains("materials");
        if (!missingTier) {
            for (String tier : MATERIAL_TIERS) {
                if (!config.contains("materials." + tier)) {
                    missingTier = true;
                    break;
                }
            }
        }

        if (missingTier) {
            logger.warning("Material tiers not found. Using default materials");
            seedDefaultMaterialTiers();
            isValid = false;
        }

        return isValid;
    }

    /**
     * Seed the four escape-time material tiers with the shipped config.yml defaults
     * (deep/medium-deep/medium/shallow core-middle-surface, per GeodePalette's default table).
     */
    private void seedDefaultMaterialTiers() {
        config.set("materials.deep.core", "CALCITE");
        config.set("materials.deep.middle", "AMETHYST_BLOCK");
        config.set("materials.deep.surface", "BUDDING_AMETHYST");

        config.set("materials.medium-deep.core", "PRISMARINE");
        config.set("materials.medium-deep.middle", "PRISMARINE_BRICKS");
        config.set("materials.medium-deep.surface", "DARK_PRISMARINE");

        config.set("materials.medium.core", "COPPER_BLOCK");
        config.set("materials.medium.surface", "OXIDIZED_COPPER");

        config.set("materials.shallow.core", "STONE");
        config.set("materials.shallow.surface", "COBBLESTONE");
    }
    
    private boolean validatePerformanceSettings() {
        boolean isValid = true;
        
        // Validate chunk generation threads
        int chunkThreads = config.getInt("performance.chunk_generation_threads", 4);
        if (chunkThreads < 1 || chunkThreads > 16) {
            logger.warning("Invalid chunk_generation_threads: " + chunkThreads + ". Setting to default 4");
            config.set("performance.chunk_generation_threads", 4);
            isValid = false;
        }
        
        // Validate cache size
        int cacheSize = config.getInt("performance.fractal_cache_size", 1000);
        if (cacheSize < 100 || cacheSize > 10000) {
            logger.warning("Invalid fractal_cache_size: " + cacheSize + ". Setting to default 1000");
            config.set("performance.fractal_cache_size", 1000);
            isValid = false;
        }
        
        return isValid;
    }
    
    private boolean validatePermissionSettings() {
        boolean isValid = true;
        
        // Check if permissions section exists
        if (!config.contains("permissions")) {
            logger.info("Permissions section not found. Creating default permissions");
            config.set("permissions.create_world", "geobrot.create");
            config.set("permissions.teleport", "geobrot.teleport");
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Get a validated integer value from config
     * 
     * @param path Configuration path
     * @param defaultValue Default value if not found or invalid
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Validated integer value
     */
    public int getValidatedInt(String path, int defaultValue, int min, int max) {
        int value = config.getInt(path, defaultValue);
        if (value < min || value > max) {
            logger.warning("Invalid value for " + path + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Get a validated double value from config
     * 
     * @param path Configuration path
     * @param defaultValue Default value if not found or invalid
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Validated double value
     */
    public double getValidatedDouble(String path, double defaultValue, double min, double max) {
        double value = config.getDouble(path, defaultValue);
        if (value < min || value > max) {
            logger.warning("Invalid value for " + path + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Get a validated string value from config
     * 
     * @param path Configuration path
     * @param defaultValue Default value if not found or invalid
     * @return Validated string value
     */
    public String getValidatedString(String path, String defaultValue) {
        String value = config.getString(path, defaultValue);
        if (value == null || value.trim().isEmpty()) {
            logger.warning("Invalid value for " + path + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
        return value.trim();
    }
}
