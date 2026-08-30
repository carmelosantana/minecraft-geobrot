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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.xpfarm.geobrot.utils.FractalMath;

/**
 * Unit tests for {@link TerrainProfile}: the immutable holder of the locked terrain numbers
 * (floor/surface/relief/iterations/bedrock) and their config parsing.
 */
class TerrainProfileTest {

    @Test
    void surfaceYForFullEscapeTimeAddsFullReliefAmplitude() {
        TerrainProfile profile = TerrainProfile.defaultProfile();

        assertEquals(165, profile.surfaceYFor(100));
    }

    @Test
    void surfaceYForZeroEscapeTimeIsSurfaceBase() {
        TerrainProfile profile = TerrainProfile.defaultProfile();

        assertEquals(153, profile.surfaceYFor(0));
    }

    @Test
    void surfaceYForHalfEscapeTimeRoundsToNearestBlock() {
        TerrainProfile profile = TerrainProfile.defaultProfile();

        // 153 + round(12 * 50 / 100) == 153 + round(6.0) == 159
        assertEquals(159, profile.surfaceYFor(50));
    }

    @Test
    void surfaceYForRoundsUpAtTheHighEnd() {
        TerrainProfile profile = TerrainProfile.defaultProfile();

        // 153 + round(12 * 96 / 100) == 153 + round(11.52) == 153 + 12 == 165
        assertEquals(165, profile.surfaceYFor(96));
    }

    @Test
    void surfaceYForRoundsDownAtTheLowEnd() {
        TerrainProfile profile = TerrainProfile.defaultProfile();

        // 153 + round(12 * 4 / 100) == 153 + round(0.48) == 153 + 0 == 153
        assertEquals(153, profile.surfaceYFor(4));
    }

    @Test
    void defaultProfileHasTheLockedTerrainNumbers() {
        TerrainProfile profile = TerrainProfile.defaultProfile();

        assertEquals(135, profile.floorY());
        assertEquals(153, profile.surfaceBaseY());
        assertEquals(12, profile.reliefAmplitude());
        assertEquals(FractalMath.getMaxIterations(), profile.maxIterations());
        assertEquals(2, profile.bedrockThickness());
        assertEquals(165, profile.maxSurfaceY());
    }

    @Test
    void fromConfigReadsGenerationKeysAndKeepsFixedFields() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("generation.floor-y", 140);
        yaml.set("generation.surface-base-y", 160);
        yaml.set("generation.relief-amplitude", 20);

        TerrainProfile profile = TerrainProfile.fromConfig(yaml);

        assertEquals(140, profile.floorY());
        assertEquals(160, profile.surfaceBaseY());
        assertEquals(20, profile.reliefAmplitude());
        assertEquals(FractalMath.getMaxIterations(), profile.maxIterations());
        assertEquals(2, profile.bedrockThickness());
    }

    @Test
    void fromConfigOnAnEmptyConfigReturnsTheDefaults() {
        YamlConfiguration yaml = new YamlConfiguration();

        TerrainProfile profile = TerrainProfile.fromConfig(yaml);

        assertEquals(135, profile.floorY());
        assertEquals(153, profile.surfaceBaseY());
        assertEquals(12, profile.reliefAmplitude());
        assertEquals(FractalMath.getMaxIterations(), profile.maxIterations());
        assertEquals(2, profile.bedrockThickness());
    }
}
