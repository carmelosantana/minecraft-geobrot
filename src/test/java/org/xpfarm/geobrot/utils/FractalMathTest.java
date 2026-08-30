package org.xpfarm.geobrot.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FractalMath utility class
 */
class FractalMathTest {
    
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        // Reset any static state if needed
    }
    
    @Test
    void testMandelbrotEscapeTime_InsideSet() {
        // Test points known to be inside the Mandelbrot set
        // The point (0, 0) should not escape
        int iterations = FractalMath.mandelbrotEscapeTime(0.0, 0.0);
        assertEquals(FractalMath.getMaxIterations(), iterations, "Point (0, 0) should not escape");
        
        // The point (-1, 0) should not escape
        iterations = FractalMath.mandelbrotEscapeTime(-1.0, 0.0);
        assertEquals(FractalMath.getMaxIterations(), iterations, "Point (-1, 0) should not escape");
        
        // The point (-0.5, 0) should not escape
        iterations = FractalMath.mandelbrotEscapeTime(-0.5, 0.0);
        assertEquals(FractalMath.getMaxIterations(), iterations, "Point (-0.5, 0) should not escape");
    }
    
    @Test
    void testMandelbrotEscapeTime_OutsideSet() {
        // Test points known to be outside the Mandelbrot set
        // The point (2, 0) should escape quickly
        int iterations = FractalMath.mandelbrotEscapeTime(2.0, 0.0);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point (2, 0) should escape");
        assertTrue(iterations > 0, "Point (2, 0) should escape after at least 1 iteration");
        
        // The point (0, 2) should escape quickly
        iterations = FractalMath.mandelbrotEscapeTime(0.0, 2.0);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point (0, 2) should escape");
        
        // The point (1, 1) should escape
        iterations = FractalMath.mandelbrotEscapeTime(1.0, 1.0);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point (1, 1) should escape");
    }
    
    @Test
    void testMandelbrotEscapeTime_EdgeCases() {
        // Test known boundary cases
        // Point (-2, 0) is on the boundary
        int iterations = FractalMath.mandelbrotEscapeTime(-2.0, 0.0);
        assertEquals(FractalMath.getMaxIterations(), iterations, "Point (-2, 0) should be in set");
        
        // Test with very large coordinates
        iterations = FractalMath.mandelbrotEscapeTime(1000.0, 1000.0);
        assertEquals(1, iterations, "Very large coordinates should escape immediately");
        
        // Test point just outside the set
        iterations = FractalMath.mandelbrotEscapeTime(0.5, 0.5);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point (0.5, 0.5) should escape");
    }
    
    @Test
    void testWorldToFractal_BasicMapping() {
        // Test center of world maps to fractal center
        double[] fractal = FractalMath.worldToFractal(500, 500, 0.0, 0.0, 1.0, 1000);
        assertEquals(0.0, fractal[0], EPSILON, "Center X should map to fractal center");
        assertEquals(0.0, fractal[1], EPSILON, "Center Y should map to fractal center");
        
        // Test world edge mapping
        double[] fractal2 = FractalMath.worldToFractal(1000, 1000, 0.0, 0.0, 1.0, 1000);
        assertEquals(1.0, fractal2[0], EPSILON, "World edge should map to fractal edge");
        assertEquals(1.0, fractal2[1], EPSILON, "World edge should map to fractal edge");
        
        // Test negative coordinates
        double[] fractal3 = FractalMath.worldToFractal(0, 0, 0.0, 0.0, 1.0, 1000);
        assertEquals(-1.0, fractal3[0], EPSILON, "World origin should map to negative fractal coords");
        assertEquals(-1.0, fractal3[1], EPSILON, "World origin should map to negative fractal coords");
    }
    
    @Test
    void testWorldToFractal_WithCenterOffset() {
        // Test with non-zero center
        double[] fractal = FractalMath.worldToFractal(500, 500, -0.5, 0.5, 1.0, 1000);
        assertEquals(-0.5, fractal[0], EPSILON, "Center offset should be applied to X");
        assertEquals(0.5, fractal[1], EPSILON, "Center offset should be applied to Y");
        
        // Test with center offset and world coordinates
        double[] fractal2 = FractalMath.worldToFractal(750, 250, -0.5, 0.5, 1.0, 1000);
        assertEquals(0.0, fractal2[0], EPSILON, "Center offset and world coords should combine");
        assertEquals(0.0, fractal2[1], EPSILON, "Center offset and world coords should combine");
    }
    
    @Test
    void testWorldToFractal_WithZoom() {
        // Test with zoom factor
        double[] fractal = FractalMath.worldToFractal(1000, 1000, 0.0, 0.0, 2.0, 1000);
        assertEquals(0.5, fractal[0], EPSILON, "Zoom should scale coordinates");
        assertEquals(0.5, fractal[1], EPSILON, "Zoom should scale coordinates");
        
        // Test with fractional zoom
        double[] fractal2 = FractalMath.worldToFractal(750, 750, 0.0, 0.0, 0.5, 1000);
        assertEquals(1.0, fractal2[0], EPSILON, "Fractional zoom should scale coordinates");
        assertEquals(1.0, fractal2[1], EPSILON, "Fractional zoom should scale coordinates");
    }
    
    @Test
    void testSeedToFractalParams_Consistency() {
        // Test that same string produces same parameters
        double[] params1 = FractalMath.seedToFractalParams("test");
        double[] params2 = FractalMath.seedToFractalParams("test");
        
        assertEquals(params1[0], params2[0], EPSILON, "Same string should produce same X center");
        assertEquals(params1[1], params2[1], EPSILON, "Same string should produce same Y center");
        assertEquals(params1[2], params2[2], EPSILON, "Same string should produce same zoom");
        
        // Test that different strings produce different parameters
        double[] params3 = FractalMath.seedToFractalParams("different");
        assertFalse(params1[0] == params3[0] && params1[1] == params3[1] && params1[2] == params3[2], 
            "Different strings should produce different parameters");
    }
    
    @Test
    void testSeedToFractalParams_EdgeCases() {
        // Test empty string
        double[] params = FractalMath.seedToFractalParams("");
        assertEquals(3, params.length, "Should return 3 parameters");
        
        // Test null string
        double[] nullParams = FractalMath.seedToFractalParams(null);
        assertEquals(3, nullParams.length, "Should handle null gracefully");
        
        // Test very long string
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("a");
        }
        params = FractalMath.seedToFractalParams(longString.toString());
        assertEquals(3, params.length, "Long string should produce valid parameters");
    }
    
    @Test
    void testSeedToFractalParams_ReasonableRange() {
        // Test that generated parameters are within reasonable fractal bounds
        for (int i = 0; i < 100; i++) {
            String seed = "test" + i;
            double[] params = FractalMath.seedToFractalParams(seed);
            
            assertTrue(params[0] >= -2.5 && params[0] <= 1.5, 
                "X center should be within reasonable fractal bounds");
            assertTrue(params[1] >= -2.0 && params[1] <= 2.0, 
                "Y center should be within reasonable fractal bounds");
            assertTrue(params[2] > 0.0 && params[2] <= 10.0, 
                "Zoom should be positive and reasonable");
        }
    }
    
    @Test
    void testComplexArithmetic_Integration() {
        // Test the integration of all methods by computing known points
        // Use a known point that's definitely outside the set
        double testX = 1.0;
        double testY = 1.0;
        
        int iterations = FractalMath.mandelbrotEscapeTime(testX, testY);
        assertTrue(iterations > 0, "Known escaping point should have positive iteration count");
        assertTrue(iterations < FractalMath.getMaxIterations(), "Known escaping point should eventually escape");
        
        // Test isInMandelbrotSet method
        assertFalse(FractalMath.isInMandelbrotSet(testX, testY), "Known escaping point should not be in set");
        assertTrue(FractalMath.isInMandelbrotSet(0.0, 0.0), "Origin should be in set");
        
        // Test world-to-fractal conversion with reasonable parameters
        double[] fractal = FractalMath.worldToFractal(600, 700, testX, testY, 1.0, 1000);
        assertTrue(fractal[0] > testX - 1.0 && fractal[0] < testX + 1.0, "World-to-fractal conversion should be reasonable");
        assertTrue(fractal[1] > testY - 1.0 && fractal[1] < testY + 1.0, "World-to-fractal conversion should be reasonable");
        
        // Test smooth escape time
        double smoothIterations = FractalMath.mandelbrotSmoothEscapeTime(testX, testY);
        assertTrue(smoothIterations >= iterations, "Smooth escape time should be >= regular escape time");
        assertTrue(smoothIterations <= iterations + 1, "Smooth escape time should be <= regular escape time + 1");
    }
    
    @Test
    void testJuliaEscapeTime() {
        // Test Julia set with a known constant
        double cx = -0.7;
        double cy = 0.27015;
        
        // Test a point that should escape
        int iterations = FractalMath.juliaEscapeTime(0.5, 0.5, cx, cy);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point should escape for Julia set");
        
        // Test a point that should not escape
        iterations = FractalMath.juliaEscapeTime(0.0, 0.0, cx, cy);
        assertTrue(iterations >= 0, "Escape time should be non-negative");
    }
    
    @Test
    void testBurningShipEscapeTime() {
        // Test Burning Ship fractal
        // Test a point that should escape
        int iterations = FractalMath.burningShipEscapeTime(1.0, 1.0);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point should escape for Burning Ship");
        
        // Test origin
        iterations = FractalMath.burningShipEscapeTime(0.0, 0.0);
        assertTrue(iterations >= 0, "Escape time should be non-negative");
    }
    
    @Test
    void testTricornEscapeTime() {
        // Test Tricorn fractal
        // Test a point that should escape
        int iterations = FractalMath.tricornEscapeTime(1.0, 1.0);
        assertTrue(iterations < FractalMath.getMaxIterations(), "Point should escape for Tricorn");
        
        // Test origin
        iterations = FractalMath.tricornEscapeTime(0.0, 0.0);
        assertTrue(iterations >= 0, "Escape time should be non-negative");
    }
    
    @Test
    void testNormalizeFractalType() {
        // Test various fractal type normalizations
        assertEquals("mandelbrot", FractalMath.normalizeFractalType("mandelbrot"));
        assertEquals("mandelbrot", FractalMath.normalizeFractalType("Mandelbrot"));
        assertEquals("mandelbrot", FractalMath.normalizeFractalType("MANDELBROT"));
        assertEquals("mandelbrot", FractalMath.normalizeFractalType("mandel"));
        assertEquals("mandelbrot", FractalMath.normalizeFractalType("Mandel"));
        
        assertEquals("julia", FractalMath.normalizeFractalType("julia"));
        assertEquals("julia", FractalMath.normalizeFractalType("Julia"));
        assertEquals("julia", FractalMath.normalizeFractalType("JULIA"));
        
        assertEquals("burning_ship", FractalMath.normalizeFractalType("burning_ship"));
        assertEquals("burning_ship", FractalMath.normalizeFractalType("burningship"));
        assertEquals("burning_ship", FractalMath.normalizeFractalType("ship"));
        
        assertEquals("tricorn", FractalMath.normalizeFractalType("tricorn"));
        assertEquals("tricorn", FractalMath.normalizeFractalType("Tricorn"));
        
        // Test defaults
        assertEquals("mandelbrot", FractalMath.normalizeFractalType(null));
        assertEquals("mandelbrot", FractalMath.normalizeFractalType(""));
        assertEquals("mandelbrot", FractalMath.normalizeFractalType("invalid"));
    }
}
