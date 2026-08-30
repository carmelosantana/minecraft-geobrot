/*
 * GeoBrot - Generate and explore floating island worlds shaped like the Mandelbrot or
 * Buddhabrot fractals.
 * Copyright (C) 2026 Carmelo Santana
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * See the LICENSE file at the project root for the full license text.
 */
package org.xpfarm.geobrot.worlds;

import org.bukkit.configuration.file.FileConfiguration;
import org.xpfarm.geobrot.utils.FractalMath;

/**
 * Immutable holder of the locked terrain shape numbers used by the fractal generator:
 * the floor and surface heights, the amount of relief the escape-time gradient carves into
 * the surface, the iteration ceiling the relief is normalized against, and the bedrock
 * thickness above the floor.
 *
 * <p>Owns both the locked defaults and the {@code generation.*} config parsing, so the
 * generator itself only ever asks a {@code TerrainProfile} "what height", never touches
 * {@link FileConfiguration} directly.
 */
public final class TerrainProfile {

    private static final int DEFAULT_FLOOR_Y = 135;
    private static final int DEFAULT_SURFACE_BASE_Y = 153;
    private static final int DEFAULT_RELIEF_AMPLITUDE = 12;
    private static final int DEFAULT_BEDROCK_THICKNESS = 2;

    private final int floorY;
    private final int surfaceBaseY;
    private final int reliefAmplitude;
    private final int maxIterations;
    private final int bedrockThickness;

    /**
     * Create a terrain profile from explicit values.
     *
     * @param floorY the lowest Y the generator ever places solid terrain at
     * @param surfaceBaseY the surface height at escape-time 0
     * @param reliefAmplitude the maximum number of blocks the escape-time gradient adds on
     *                        top of {@code surfaceBaseY}
     * @param maxIterations the escape-time ceiling {@code surfaceYFor} normalizes against
     * @param bedrockThickness the bedrock layer thickness immediately above {@code floorY}
     */
    public TerrainProfile(int floorY, int surfaceBaseY, int reliefAmplitude, int maxIterations,
            int bedrockThickness) {
        this.floorY = floorY;
        this.surfaceBaseY = surfaceBaseY;
        this.reliefAmplitude = reliefAmplitude;
        this.maxIterations = maxIterations;
        this.bedrockThickness = bedrockThickness;
    }

    /**
     * The locked default profile: floorY 135, surfaceBaseY 153, reliefAmplitude 12,
     * maxIterations from {@link FractalMath#getMaxIterations()}, bedrockThickness 2.
     */
    public static TerrainProfile defaultProfile() {
        return new TerrainProfile(DEFAULT_FLOOR_Y, DEFAULT_SURFACE_BASE_Y,
                DEFAULT_RELIEF_AMPLITUDE, FractalMath.getMaxIterations(),
                DEFAULT_BEDROCK_THICKNESS);
    }

    /**
     * Build a profile from {@code generation.floor-y}, {@code generation.surface-base-y}, and
     * {@code generation.relief-amplitude}, falling back to the locked defaults for any missing
     * key. {@code maxIterations} always comes from {@link FractalMath#getMaxIterations()} and
     * {@code bedrockThickness} is always the locked default: neither is configurable.
     */
    public static TerrainProfile fromConfig(FileConfiguration config) {
        int floorY = config.getInt("generation.floor-y", DEFAULT_FLOOR_Y);
        int surfaceBaseY = config.getInt("generation.surface-base-y", DEFAULT_SURFACE_BASE_Y);
        int reliefAmplitude = config.getInt("generation.relief-amplitude", DEFAULT_RELIEF_AMPLITUDE);

        return new TerrainProfile(floorY, surfaceBaseY, reliefAmplitude,
                FractalMath.getMaxIterations(), DEFAULT_BEDROCK_THICKNESS);
    }

    public int floorY() {
        return floorY;
    }

    public int surfaceBaseY() {
        return surfaceBaseY;
    }

    public int reliefAmplitude() {
        return reliefAmplitude;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public int bedrockThickness() {
        return bedrockThickness;
    }

    /**
     * The surface Y for a given escape-time: {@code surfaceBaseY} plus a share of
     * {@code reliefAmplitude} proportional to how close the point is to {@code maxIterations}.
     */
    public int surfaceYFor(int escapeTime) {
        return surfaceBaseY + (int) Math.round(reliefAmplitude * escapeTime / (double) maxIterations);
    }

    /**
     * The highest surface Y this profile can ever produce (at escape-time == maxIterations).
     * Used as the spawn-fallback ceiling.
     */
    public int maxSurfaceY() {
        return surfaceBaseY + reliefAmplitude;
    }
}
