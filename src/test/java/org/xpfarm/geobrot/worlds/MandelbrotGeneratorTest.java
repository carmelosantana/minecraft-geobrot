package org.xpfarm.geobrot.worlds;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.xpfarm.geobrot.utils.FractalMath;

/**
 * Unit tests for MandelbrotGenerator's world-to-fractal coordinate mapping.
 *
 * These pin the centered mapping via {@code fractalCoordsFor}, which adopts
 * {@link FractalMath#worldToFractal(int, int, double, double, double, int)} so that world
 * origin (0,0) maps to the fractal center, producing a recognizable, centered Mandelbrot
 * rather than a lopsided sliver.
 */
class MandelbrotGeneratorTest {

    private static final int FRACTAL_WORLD_SIZE = 512;
    private static final double DELTA = 1e-9;

    @Test
    void originMapsToFractalCenter() {
        MandelbrotGenerator generator = new MandelbrotGenerator(-0.7, 0.0, 1.0, 512);

        double[] fractal = generator.fractalCoordsFor(0, 0);

        assertEquals(-0.7, fractal[0], DELTA, "world origin X should map to centerX");
        assertEquals(0.0, fractal[1], DELTA, "world origin Z should map to centerY");
    }

    @Test
    void mappingIsSymmetricAboutTheOrigin() {
        MandelbrotGenerator generator = new MandelbrotGenerator(-0.7, 0.0, 1.0, 512);
        int d = 128;

        double positiveOffset = generator.fractalCoordsFor(d, 0)[0] - generator.getCenterX();
        double negativeOffset = generator.fractalCoordsFor(-d, 0)[0] - generator.getCenterX();

        assertEquals(positiveOffset, -negativeOffset, DELTA,
            "world points equidistant from origin should be equidistant (opposite sign) from fractal center");
    }

    @Test
    void worldWindowEdgeMapsToOneZoomedUnit() {
        double zoom = 1.0;
        MandelbrotGenerator generator = new MandelbrotGenerator(-0.7, 0.0, zoom, 512);

        double[] fractal = generator.fractalCoordsFor(FRACTAL_WORLD_SIZE / 2, 0);

        assertEquals(generator.getCenterX() + 1.0 / zoom, fractal[0], DELTA,
            "the world window edge should map to one unit of the zoomed fractal plane");
    }

    @Test
    void producesARecognizableCenteredSet() {
        MandelbrotGenerator generator = new MandelbrotGenerator(-0.7, 0.0, 1.0, 512);

        double[] originFractal = generator.fractalCoordsFor(0, 0);
        int originEscapeTime = FractalMath.mandelbrotEscapeTime(originFractal[0], originFractal[1]);
        assertEquals(FractalMath.getMaxIterations(), originEscapeTime,
            "the world origin should map into the main cardioid (in-set), so the recognizable body sits at spawn");

        double[] farFractal = generator.fractalCoordsFor(4 * FRACTAL_WORLD_SIZE, 0);
        int farEscapeTime = FractalMath.mandelbrotEscapeTime(farFractal[0], farFractal[1]);
        assertTrue(farEscapeTime < FractalMath.getMaxIterations(),
            "a far-out world point should map outside the set, proving the set is framed/bounded, not filling everything");
    }
}
