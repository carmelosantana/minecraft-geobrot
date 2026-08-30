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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.xpfarm.geobrot.utils.FractalMath;

/**
 * Unit tests for {@link FractalWorldManager#resolveFractalParams(String, org.bukkit.configuration.file.FileConfiguration)}:
 * resolving a {@code /mandel create} argument to fractal parameters, either from a named
 * {@code defaults.presets} entry or by falling back to the seed hashing behavior.
 */
class PresetResolverTest {

    private static final double DELTA = 1e-9;

    private YamlConfiguration configWithPresets() {
        YamlConfiguration config = new YamlConfiguration();

        config.set("defaults.presets.classic.center-x", -0.7);
        config.set("defaults.presets.classic.center-y", 0.0);
        config.set("defaults.presets.classic.zoom", 1.0);

        config.set("defaults.presets.spiral.center-x", -0.16);
        config.set("defaults.presets.spiral.center-y", 1.038);
        config.set("defaults.presets.spiral.zoom", 0.8);

        config.set("defaults.presets.seahorse.center-x", -0.74529);
        config.set("defaults.presets.seahorse.center-y", 0.11307);
        config.set("defaults.presets.seahorse.zoom", 1.5);

        config.set("defaults.presets.elephant.center-x", 0.25);
        config.set("defaults.presets.elephant.center-y", 0.0);
        config.set("defaults.presets.elephant.zoom", 1.2);

        return config;
    }

    @Test
    void resolvesKnownPresetByExactName() {
        YamlConfiguration config = configWithPresets();

        double[] params = FractalWorldManager.resolveFractalParams("classic", config);

        assertArrayEquals(new double[]{-0.7, 0.0, 1.0}, params, DELTA);
    }

    @Test
    void resolvesKnownPresetCaseInsensitively() {
        YamlConfiguration config = configWithPresets();

        double[] params = FractalWorldManager.resolveFractalParams("SEAHORSE", config);

        assertArrayEquals(new double[]{-0.74529, 0.11307, 1.5}, params, DELTA);
    }

    @Test
    void unknownArgFallsBackToSeedHashing() {
        YamlConfiguration config = configWithPresets();

        double[] params = FractalWorldManager.resolveFractalParams("somerandomseed", config);

        assertArrayEquals(FractalMath.seedToFractalParams("somerandomseed"), params, DELTA);
    }

    @Test
    void nullArgResolvesToDefaultLocation() {
        YamlConfiguration config = configWithPresets();

        double[] params = FractalWorldManager.resolveFractalParams(null, config);

        assertArrayEquals(FractalMath.seedToFractalParams(null), params, DELTA);
    }
}
