package org.xpfarm.geobrot.worlds;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.xpfarm.geobrot.utils.FractalMath;

/**
 * Unit tests for MandelbrotGenerator's world-to-fractal coordinate mapping and its column
 * material stack.
 *
 * The mapping tests pin the centered mapping via {@code fractalCoordsFor}, which adopts
 * {@link FractalMath#worldToFractal(int, int, double, double, double, int)} so that world
 * origin (0,0) maps to the fractal center, producing a recognizable, centered Mandelbrot
 * rather than a lopsided sliver.
 *
 * The column tests pin {@code columnMaterialAt}, the pure helper that turns a locked
 * {@link TerrainProfile} + {@link GeodePalette} into the per-block material for a column.
 */
class MandelbrotGeneratorTest {

    private static final int FRACTAL_WORLD_SIZE = 512;
    private static final double DELTA = 1e-9;

    /** Default profile + palette (an empty config resolves every material to the table default). */
    private static TerrainProfile defaultProfile() {
        return TerrainProfile.defaultProfile();
    }

    private static GeodePalette defaultPalette() {
        return GeodePalette.fromConfig(new YamlConfiguration());
    }

    private static MandelbrotGenerator generator(double centerX, double centerY, double zoom) {
        return new MandelbrotGenerator(centerX, centerY, zoom, defaultProfile(), defaultPalette());
    }

    @Test
    void originMapsToFractalCenter() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        double[] fractal = generator.fractalCoordsFor(0, 0);

        assertEquals(-0.7, fractal[0], DELTA, "world origin X should map to centerX");
        assertEquals(0.0, fractal[1], DELTA, "world origin Z should map to centerY");
    }

    @Test
    void mappingIsSymmetricAboutTheOrigin() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);
        int d = 128;

        double positiveOffset = generator.fractalCoordsFor(d, 0)[0] - generator.getCenterX();
        double negativeOffset = generator.fractalCoordsFor(-d, 0)[0] - generator.getCenterX();

        assertEquals(positiveOffset, -negativeOffset, DELTA,
            "world points equidistant from origin should be equidistant (opposite sign) from fractal center");
    }

    @Test
    void worldWindowEdgeMapsToOneZoomedUnit() {
        double zoom = 1.0;
        MandelbrotGenerator generator = generator(-0.7, 0.0, zoom);

        double[] fractal = generator.fractalCoordsFor(FRACTAL_WORLD_SIZE / 2, 0);

        assertEquals(generator.getCenterX() + 1.0 / zoom, fractal[0], DELTA,
            "the world window edge should map to one unit of the zoomed fractal plane");
    }

    @Test
    void producesARecognizableCenteredSet() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        double[] originFractal = generator.fractalCoordsFor(0, 0);
        int originEscapeTime = FractalMath.mandelbrotEscapeTime(originFractal[0], originFractal[1]);
        assertEquals(FractalMath.getMaxIterations(), originEscapeTime,
            "the world origin should map into the main cardioid (in-set), so the recognizable body sits at spawn");

        double[] farFractal = generator.fractalCoordsFor(4 * FRACTAL_WORLD_SIZE, 0);
        int farEscapeTime = FractalMath.mandelbrotEscapeTime(farFractal[0], farFractal[1]);
        assertTrue(farEscapeTime < FractalMath.getMaxIterations(),
            "a far-out world point should map outside the set, proving the set is framed/bounded, not filling everything");
    }

    // --- columnMaterialAt: in-set column (E=100, S=165) ---

    @Test
    void inSetColumnSurfaceIsGrass() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.GRASS_BLOCK, generator.columnMaterialAt(165, 165, 100));
    }

    @Test
    void inSetColumnDirtLayerIsThreeBlocksBelowSurface() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.DIRT, generator.columnMaterialAt(164, 165, 100));
        assertEquals(Material.DIRT, generator.columnMaterialAt(163, 165, 100));
        assertEquals(Material.DIRT, generator.columnMaterialAt(162, 165, 100));
    }

    @Test
    void inSetColumnGeodeBandTopIsDeepTierSurfaceMaterial() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        // gTop = surfaceY - 4 = 161; deep tier (E=100 > 70) surface material is BUDDING_AMETHYST.
        assertEquals(Material.BUDDING_AMETHYST, generator.columnMaterialAt(161, 165, 100));
    }

    @Test
    void inSetColumnGeodeBandCoreNearBottomIsDeepTierCoreMaterial() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        // y=137 is the deepest geode block (just above bedrock); deep tier core is CALCITE.
        assertEquals(Material.CALCITE, generator.columnMaterialAt(137, 165, 100));
    }

    @Test
    void inSetColumnBedrockCapIsBottomTwoBlocks() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.BEDROCK, generator.columnMaterialAt(135, 165, 100));
        assertEquals(Material.BEDROCK, generator.columnMaterialAt(136, 165, 100));
    }

    @Test
    void inSetColumnIsAirBelowFloorAndAboveSurface() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.AIR, generator.columnMaterialAt(134, 165, 100), "below floorY must be air, no plinth");
        assertEquals(Material.AIR, generator.columnMaterialAt(166, 165, 100), "above surface must be air");
    }

    @Test
    void inSetColumnThicknessIsThirtyOneSolidBlocksFloorToSurface() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        int solidCount = 0;
        for (int y = 135; y <= 165; y++) {
            if (generator.columnMaterialAt(y, 165, 100) != Material.AIR) {
                solidCount++;
            }
        }

        assertEquals(31, solidCount, "floor (135) through surface (165) inclusive must be fully solid, no plinth gaps");
        assertEquals(Material.AIR, generator.columnMaterialAt(134, 165, 100), "one block below floor must be air");
    }

    // --- columnMaterialAt: fringe column (E=0, S=153) ---

    @Test
    void fringeColumnSurfaceIsGrass() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.GRASS_BLOCK, generator.columnMaterialAt(153, 153, 0));
    }

    @Test
    void fringeColumnDirtLayerIsThreeBlocksBelowSurface() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.DIRT, generator.columnMaterialAt(152, 153, 0));
        assertEquals(Material.DIRT, generator.columnMaterialAt(151, 153, 0));
        assertEquals(Material.DIRT, generator.columnMaterialAt(150, 153, 0));
    }

    @Test
    void fringeColumnGeodeBandUsesShallowTier() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        // gTop = surfaceY - 4 = 149; shallow tier (E=0 < 30) surface material is COBBLESTONE.
        assertEquals(Material.COBBLESTONE, generator.columnMaterialAt(149, 153, 0));
        // y=137 is the deepest geode block; shallow tier core is STONE.
        assertEquals(Material.STONE, generator.columnMaterialAt(137, 153, 0));
    }

    @Test
    void fringeColumnBedrockAndAir() {
        MandelbrotGenerator generator = generator(-0.7, 0.0, 1.0);

        assertEquals(Material.BEDROCK, generator.columnMaterialAt(135, 153, 0));
        assertEquals(Material.BEDROCK, generator.columnMaterialAt(136, 153, 0));
        assertEquals(Material.AIR, generator.columnMaterialAt(134, 153, 0));
    }
}
