package org.xpfarm.geobrot;

import org.bukkit.plugin.java.JavaPlugin;
import org.xpfarm.geobrot.commands.MandelCommand;
import org.xpfarm.geobrot.config.ConfigValidator;
import org.xpfarm.geobrot.worlds.FractalWorldManager;

import java.util.logging.Logger;

/**
 * GeoBrot Plugin - Generate and explore floating island worlds shaped like fractals
 * 
 * This plugin creates beautiful 2D fractal worlds based on the Mandelbrot set,
 * allowing players to explore stunning mathematical landscapes as floating islands.
 */
public class GeoBrotPlugin extends JavaPlugin {
    
    private FractalWorldManager worldManager;
    private ConfigValidator configValidator;
    private static GeoBrotPlugin instance;
    private static final Logger logger = Logger.getLogger("GeoBrot");
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Create plugin data directory
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Load and validate configuration
        saveDefaultConfig();
        configValidator = new ConfigValidator(this);
        configValidator.validateConfig();
        
        // Initialize world manager
        worldManager = new FractalWorldManager(this);
        
        // Register commands
        registerCommands();
        
        // Log successful loading
        logger.info("GeoBrot Plugin v" + getDescription().getVersion() + " enabled!");
        logger.info("Ready to generate fractal worlds!");
    }
    
    @Override
    public void onDisable() {
        if (worldManager != null) {
            worldManager.shutdown();
        }
        
        logger.info("GeoBrot Plugin disabled!");
        instance = null;
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        MandelCommand mandelCommand = new MandelCommand(this);
        getCommand("mandel").setExecutor(mandelCommand);
        getCommand("mandel").setTabCompleter(mandelCommand);
    }
    
    /**
     * Get the plugin instance
     * @return Plugin instance
     */
    public static GeoBrotPlugin getInstance() {
        return instance;
    }
    
    /**
     * Get the fractal world manager
     * @return World manager instance
     */
    public FractalWorldManager getWorldManager() {
        return worldManager;
    }
    
    /**
     * Get the configuration validator
     * @return Configuration validator instance
     */
    public ConfigValidator getConfigValidator() {
        return configValidator;
    }
    
    /**
     * Get the plugin logger
     * @return Logger instance
     */
    public static Logger getPluginLogger() {
        return logger;
    }
}
