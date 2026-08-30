package org.xpfarm.geobrot.worlds;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
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
    private final TerrainProfile profile;
    private final GeodePalette palette;

    /**
     * Create a new Mandelbrot generator
     *
     * @param centerX Center X coordinate in fractal space
     * @param centerY Center Y coordinate in fractal space
     * @param zoom Zoom level (higher = more zoomed in)
     * @param profile the locked terrain shape (floor/surface heights, relief) this generator builds columns from
     * @param palette the resolved geode materials this generator picks from within the geode band
     */
    public MandelbrotGenerator(double centerX, double centerY, double zoom, TerrainProfile profile,
            GeodePalette palette) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.zoom = zoom;
        this.profile = profile;
        this.palette = palette;
    }

    /**
     * Default constructor with reasonable defaults
     */
    public MandelbrotGenerator() {
        this(0.0, 0.0, 1.0, TerrainProfile.defaultProfile(), GeodePalette.fromConfig(new YamlConfiguration()));
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

                // Convert escape time to the locked surface height for this column
                int surfaceY = profile.surfaceYFor(escapeTime);

                // Generate terrain column
                generateColumn(chunkData, x, z, surfaceY, escapeTime);
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
     * Pure column-material lookup for the locked vertical model: below the floor or above the
     * surface is air (no plinth); the bottom {@code bedrockThickness} blocks above the floor are
     * bedrock; the surface block is grass; the three blocks below that are dirt; everything else
     * down to the floor is the geode band, colored by {@link GeodePalette#geodeMaterialAt}.
     *
     * @param y the block Y being resolved
     * @param surfaceY this column's surface height ({@link TerrainProfile#surfaceYFor(int)})
     * @param escapeTime this column's Mandelbrot escape-time, selecting the geode tier
     */
    Material columnMaterialAt(int y, int surfaceY, int escapeTime) {
        int floorY = profile.floorY();

        if (y < floorY || y > surfaceY) {
            return Material.AIR;
        }
        if (y < floorY + profile.bedrockThickness()) {
            return Material.BEDROCK;
        }
        if (y == surfaceY) {
            return Material.GRASS_BLOCK;
        }
        if (y >= surfaceY - 3) {
            return Material.DIRT;
        }

        int gTop = surfaceY - 4;
        int bandHeight = gTop - (floorY + profile.bedrockThickness()) + 1;
        int depthFromTop = gTop - y;
        return palette.geodeMaterialAt(escapeTime, depthFromTop, bandHeight);
    }

    /**
     * Generate a single terrain column from the floor up to this column's surface height,
     * clamped to the chunk's min/max height. Air blocks are never set (below the floor or above
     * the surface never happens here since the scan range is already floor..surface).
     */
    private void generateColumn(@NotNull ChunkData chunkData, int x, int z, int surfaceY, int escapeTime) {
        int minY = Math.max(profile.floorY(), chunkData.getMinHeight());
        int maxY = Math.min(surfaceY, chunkData.getMaxHeight() - 1);

        for (int y = minY; y <= maxY; y++) {
            Material material = columnMaterialAt(y, surfaceY, escapeTime);
            if (material != Material.AIR) {
                chunkData.setBlock(x, y, z, material);
            }
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
        return new Location(world, centerX + 0.5, profile.maxSurfaceY() + 2, centerZ + 0.5);
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
}
