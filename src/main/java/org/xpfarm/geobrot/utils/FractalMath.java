package org.xpfarm.geobrot.utils;

/**
 * Fractal mathematics utility class
 * 
 * Provides methods for calculating fractal values, particularly the Mandelbrot set,
 * used to generate the shape of floating island worlds.
 */
public class FractalMath {
    
    /**
     * Maximum iterations for fractal calculation
     */
    private static final int MAX_ITERATIONS = 100;
    
    /**
     * Escape radius squared (optimization to avoid sqrt)
     */
    private static final double ESCAPE_RADIUS_SQUARED = 4.0;
    
    /**
     * Calculate the Mandelbrot set escape-time for a given complex number
     * 
     * @param x Real part of complex number
     * @param y Imaginary part of complex number
     * @return Escape-time value (0 to MAX_ITERATIONS)
     */
    public static int mandelbrotEscapeTime(double x, double y) {
        double zx = 0.0;
        double zy = 0.0;
        int iteration = 0;
        
        while (iteration < MAX_ITERATIONS) {
            // Calculate z^2 + c
            double zx2 = zx * zx;
            double zy2 = zy * zy;
            
            // Check if we've escaped
            if (zx2 + zy2 > ESCAPE_RADIUS_SQUARED) {
                break;
            }
            
            // z = z^2 + c
            zy = 2.0 * zx * zy + y;
            zx = zx2 - zy2 + x;
            
            iteration++;
        }
        
        return iteration;
    }
    
    /**
     * Calculate smooth escape-time for better gradients
     * 
     * @param x Real part of complex number
     * @param y Imaginary part of complex number
     * @return Smooth escape-time value
     */
    public static double mandelbrotSmoothEscapeTime(double x, double y) {
        double zx = 0.0;
        double zy = 0.0;
        int iteration = 0;
        
        while (iteration < MAX_ITERATIONS) {
            double zx2 = zx * zx;
            double zy2 = zy * zy;
            
            if (zx2 + zy2 > ESCAPE_RADIUS_SQUARED) {
                // Smooth coloring
                double log_zn = Math.log(zx2 + zy2) / 2.0;
                double nu = Math.log(log_zn / Math.log(2.0)) / Math.log(2.0);
                return iteration + 1.0 - nu;
            }
            
            zy = 2.0 * zx * zy + y;
            zx = zx2 - zy2 + x;
            
            iteration++;
        }
        
        return iteration;
    }
    
    /**
     * Check if a point is inside the Mandelbrot set
     * 
     * @param x Real part of complex number
     * @param y Imaginary part of complex number
     * @return True if point is in the set (didn't escape)
     */
    public static boolean isInMandelbrotSet(double x, double y) {
        return mandelbrotEscapeTime(x, y) == MAX_ITERATIONS;
    }
    
    /**
     * Map world coordinates to fractal coordinates
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param centerX Fractal center X
     * @param centerY Fractal center Y
     * @param zoom Zoom level (higher = more zoomed in)
     * @param worldSize Size of the world area
     * @return Array with [fractalX, fractalY]
     */
    public static double[] worldToFractal(int worldX, int worldZ, double centerX, double centerY, 
                                         double zoom, int worldSize) {
        // Normalize world coordinates to [-1, 1] range
        double normalizedX = (worldX - worldSize / 2.0) / (worldSize / 2.0);
        double normalizedZ = (worldZ - worldSize / 2.0) / (worldSize / 2.0);
        
        // Apply zoom and center
        double fractalX = normalizedX / zoom + centerX;
        double fractalY = normalizedZ / zoom + centerY;
        
        return new double[]{fractalX, fractalY};
    }
    
    /**
     * Generate a fractal seed from a string
     * 
     * @param seedString String to convert to fractal parameters
     * @return Array with [centerX, centerY, zoom]
     */
    public static double[] seedToFractalParams(String seedString) {
        if (seedString == null || seedString.isEmpty()) {
            // Default interesting location
            return new double[]{-0.7, 0.0, 1.0};
        }
        
        // Use string hash to generate consistent parameters
        int hash = seedString.hashCode();
        
        // Generate center coordinates (interesting region of Mandelbrot set)
        double centerX = -2.0 + (hash & 0x7FFF) / 32767.0 * 3.0; // Range: -2.0 to 1.0
        double centerY = -1.5 + ((hash >> 16) & 0x7FFF) / 32767.0 * 3.0; // Range: -1.5 to 1.5
        
        // Generate zoom level
        double zoom = 0.5 + (Math.abs(hash) % 1000) / 1000.0 * 2.0; // Range: 0.5 to 2.5
        
        return new double[]{centerX, centerY, zoom};
    }
    
    /**
     * Get the maximum iteration count
     * 
     * @return Maximum iterations
     */
    public static int getMaxIterations() {
        return MAX_ITERATIONS;
    }
    
    /**
     * Calculate the Julia set escape-time for a given complex number
     * 
     * @param x Real part of complex number
     * @param y Imaginary part of complex number
     * @param cx Real part of Julia set constant
     * @param cy Imaginary part of Julia set constant
     * @return Escape-time value (0 to MAX_ITERATIONS)
     */
    public static int juliaEscapeTime(double x, double y, double cx, double cy) {
        double zx = x;
        double zy = y;
        int iteration = 0;
        
        while (iteration < MAX_ITERATIONS) {
            double zx2 = zx * zx;
            double zy2 = zy * zy;
            
            if (zx2 + zy2 > ESCAPE_RADIUS_SQUARED) {
                break;
            }
            
            // z = z^2 + c
            zy = 2.0 * zx * zy + cy;
            zx = zx2 - zy2 + cx;
            
            iteration++;
        }
        
        return iteration;
    }
    
    /**
     * Calculate the Burning Ship fractal escape-time
     * 
     * @param x Real part of complex number
     * @param y Imaginary part of complex number
     * @return Escape-time value (0 to MAX_ITERATIONS)
     */
    public static int burningShipEscapeTime(double x, double y) {
        double zx = 0.0;
        double zy = 0.0;
        int iteration = 0;
        
        while (iteration < MAX_ITERATIONS) {
            double zx2 = zx * zx;
            double zy2 = zy * zy;
            
            if (zx2 + zy2 > ESCAPE_RADIUS_SQUARED) {
                break;
            }
            
            // z = (|Re(z)| + i|Im(z)|)^2 + c
            zy = 2.0 * Math.abs(zx) * Math.abs(zy) + y;
            zx = zx2 - zy2 + x;
            
            iteration++;
        }
        
        return iteration;
    }
    
    /**
     * Calculate the Tricorn fractal escape-time
     * 
     * @param x Real part of complex number
     * @param y Imaginary part of complex number
     * @return Escape-time value (0 to MAX_ITERATIONS)
     */
    public static int tricornEscapeTime(double x, double y) {
        double zx = 0.0;
        double zy = 0.0;
        int iteration = 0;
        
        while (iteration < MAX_ITERATIONS) {
            double zx2 = zx * zx;
            double zy2 = zy * zy;
            
            if (zx2 + zy2 > ESCAPE_RADIUS_SQUARED) {
                break;
            }
            
            // z = conj(z)^2 + c
            zy = -2.0 * zx * zy + y;
            zx = zx2 - zy2 + x;
            
            iteration++;
        }
        
        return iteration;
    }
    
    /**
     * Get fractal type from string
     * 
     * @param fractalType String representation of fractal type
     * @return Normalized fractal type string
     */
    public static String normalizeFractalType(String fractalType) {
        if (fractalType == null) {
            return "mandelbrot";
        }
        
        String normalized = fractalType.toLowerCase().trim();
        switch (normalized) {
            case "mandelbrot":
            case "mandel":
                return "mandelbrot";
            case "julia":
                return "julia";
            case "burning_ship":
            case "burningship":
            case "ship":
                return "burning_ship";
            case "tricorn":
                return "tricorn";
            default:
                return "mandelbrot";
        }
    }
}
