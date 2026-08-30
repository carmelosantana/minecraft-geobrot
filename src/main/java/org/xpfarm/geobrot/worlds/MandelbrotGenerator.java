package org.xpfarm.geobrot.worlds;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xpfarm.geobrot.utils.FractalMath;

import java.util.List;
import java.util.Random;

/**
 * Custom chunk generator for creating Mandelbrot fractal worlds
 * 
 * This generator creates terrain based on the Mandelbrot set, where:
 * - Points inside the set become solid terrain
 * - Points outside create void/air spaces
 * - The iteration count determines terrain height
 */
public class MandelbrotGenerator extends ChunkGenerator {

    /** Fixed world-space window (blocks) the fractal is framed across; the set is centered on the world origin. */
    private static final int FRACTAL_WORLD_SIZE = 512;

    private final double centerX;
    private final double centerY;
    private final double zoom;
    private final int maxHeight;
    private final int seaLevel;
    
    /**
     * Create a new Mandelbrot generator
     * 
     * @param centerX Center X coordinate in fractal space
     * @param centerY Center Y coordinate in fractal space
     * @param zoom Zoom level (higher = more zoomed in)
     * @param maxHeight Maximum terrain height
     */
    public MandelbrotGenerator(double centerX, double centerY, double zoom, int maxHeight) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.zoom = zoom;
        this.maxHeight = Math.max(1, Math.min(maxHeight, 320)); // Clamp to world height limits
        this.seaLevel = 64;
    }
    
    /**
     * Default constructor with reasonable defaults
     */
    public MandelbrotGenerator() {
        this(0.0, 0.0, 1.0, 128);
    }
    
    /**
     * Generate noise for the chunk. This is the main terrain generation method.
     */
    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, 
                             int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        
        // Generate terrain for each column in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                
                // Calculate fractal coordinates for this location
                double[] fractal = fractalCoordsFor(worldX, worldZ);
                int escapeTime = FractalMath.mandelbrotEscapeTime(fractal[0], fractal[1]);
                
                // Convert escape time to height
                int height = calculateHeight(escapeTime);
                
                // Generate terrain column
                generateColumn(chunkData, x, z, height);
            }
        }
    }
    
    /**
     * Map a world (x,z) column to its Mandelbrot-plane coordinates, with the set centered on the
     * world origin and framed/zoomed across FRACTAL_WORLD_SIZE. Adopts FractalMath.worldToFractal;
     * the +FRACTAL_WORLD_SIZE/2 offset re-centers worldToFractal's viewport (which is centered at
     * worldSize/2) onto world (0,0), so world origin == fractal center (centerX, centerY).
     * @return { fractalX, fractalZ }
     */
    double[] fractalCoordsFor(int worldX, int worldZ) {
        return FractalMath.worldToFractal(
            worldX + FRACTAL_WORLD_SIZE / 2,
            worldZ + FRACTAL_WORLD_SIZE / 2,
            centerX, centerY, zoom, FRACTAL_WORLD_SIZE);
    }

    /**
     * Calculate terrain height based on Mandelbrot escape time
     */
    private int calculateHeight(int escapeTime) {
        // FractalMath uses MAX_ITERATIONS = 100
        final int MAX_ITERATIONS = 100;
        
        if (escapeTime == MAX_ITERATIONS) {
            // Point is in the Mandelbrot set - create solid terrain
            return seaLevel + (maxHeight - seaLevel) / 2;
        } else {
            // Point escaped - height based on escape time
            double heightRatio = (double) escapeTime / MAX_ITERATIONS;
            return (int) (seaLevel * heightRatio);
        }
    }
    
    /**
     * Generate a single terrain column
     */
    private void generateColumn(@NotNull ChunkData chunkData, int x, int z, int height) {
        // Fill from bedrock to calculated height
        for (int y = chunkData.getMinHeight(); y <= Math.min(height, chunkData.getMaxHeight() - 1); y++) {
            Material material = getMaterialForHeight(y, height);
            chunkData.setBlock(x, y, z, material);
        }
    }
    
    /**
     * Determine material based on height and terrain type
     */
    private Material getMaterialForHeight(int y, int surfaceHeight) {
        if (y <= 5) {
            return Material.BEDROCK;
        } else if (y < surfaceHeight - 3) {
            return Material.STONE;
        } else if (y < surfaceHeight) {
            return Material.DIRT;
        } else if (y == surfaceHeight) {
            return surfaceHeight > seaLevel ? Material.GRASS_BLOCK : Material.SAND;
        } else {
            return Material.AIR;
        }
    }
    
    /**
     * Find a safe spawn location in this fractal world
     */
    @Nullable
    public Location findSpawnLocation(@NotNull World world) {
        // Search in expanding circles from spawn
        int centerX = 0;
        int centerZ = 0;
        int maxSearchRadius = 200;
        
        for (int radius = 0; radius < maxSearchRadius; radius += 16) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int searchX = centerX + (int)(radius * Math.cos(radians));
                int searchZ = centerZ + (int)(radius * Math.sin(radians));
                
                Location safeLocation = findSafeLocationAt(world, searchX, searchZ);
                if (safeLocation != null) {
                    return safeLocation;
                }
            }
        }
        
        // Fallback: return a location high above the center
        return new Location(world, centerX + 0.5, Math.max(100, maxHeight + 20), centerZ + 0.5);
    }
    
    /**
     * Check if a specific location is safe for spawning
     */
    @Nullable
    private Location findSafeLocationAt(@NotNull World world, int x, int z) {
        // Find the highest solid block
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();
            
            if (blockType.isSolid() && !blockType.equals(Material.LAVA)) {
                // Check if there's space above for the player
                Material above1 = world.getBlockAt(x, y + 1, z).getType();
                Material above2 = world.getBlockAt(x, y + 2, z).getType();
                
                if (above1.equals(Material.AIR) && above2.equals(Material.AIR)) {
                    // Safe location found
                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }
        
        return null; // No safe location found at this x,z
    }
    
    // Required ChunkGenerator overrides for Paper compatibility
    
    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateCaves() {
        return false; // Fractal worlds don't need caves
    }
    
    @Override
    public boolean shouldGenerateDecorations() {
        return false; // Keep fractal worlds clean
    }
    
    @Override
    public boolean shouldGenerateMobs() {
        return true; // Allow mob spawning
    }
    
    @Override
    public boolean shouldGenerateStructures() {
        return false; // No villages/dungeons in fractal worlds
    }
    
    /**
     * Provide a simple biome for the fractal world
     */
    @Override
    @Nullable
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            @NotNull
            public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
                return Biome.PLAINS; // Simple biome for fractal worlds
            }
            
            @Override
            @NotNull
            public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
                return List.of(Biome.PLAINS);
            }
        };
    }
    
    // Getters for configuration
    
    public double getCenterX() {
        return centerX;
    }
    
    public double getCenterY() {
        return centerY;
    }
    
    public double getZoom() {
        return zoom;
    }
    
    public int getMaxHeight() {
        return maxHeight;
    }
    
    public int getSeaLevel() {
        return seaLevel;
    }
}
