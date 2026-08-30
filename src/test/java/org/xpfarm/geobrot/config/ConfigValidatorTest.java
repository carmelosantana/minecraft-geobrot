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
package org.xpfarm.geobrot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConfigValidator}, driven against real, memory-backed
 * {@link YamlConfiguration} instances (no running server required) via the package-private
 * {@code ConfigValidator(FileConfiguration, Logger)} test seam.
 */
class ConfigValidatorTest {

    /** Captures every log message emitted through a freshly-created, isolated Logger. */
    private static final class CapturingLogger {
        final Logger logger;
        final List<String> messages = new ArrayList<>();

        CapturingLogger(String name) {
            logger = Logger.getLogger("ConfigValidatorTest." + name + "." + System.nanoTime());
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            logger.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                    messages.add(record.getMessage());
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
        }

        boolean anyMessageContains(String needle) {
            String lowerNeedle = needle.toLowerCase(Locale.ROOT);
            return messages.stream()
                    .anyMatch(m -> m != null && m.toLowerCase(Locale.ROOT).contains(lowerNeedle));
        }
    }

    /** The real {@code materials.*} block as shipped in config.yml. */
    private static YamlConfiguration shippedMaterialsYaml() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("materials.deep.core", "CALCITE");
        yaml.set("materials.deep.middle", "AMETHYST_BLOCK");
        yaml.set("materials.deep.surface", "BUDDING_AMETHYST");
        yaml.set("materials.medium-deep.core", "PRISMARINE");
        yaml.set("materials.medium-deep.middle", "PRISMARINE_BRICKS");
        yaml.set("materials.medium-deep.surface", "DARK_PRISMARINE");
        yaml.set("materials.medium.core", "COPPER_BLOCK");
        yaml.set("materials.medium.surface", "OXIDIZED_COPPER");
        yaml.set("materials.shallow.core", "STONE");
        yaml.set("materials.shallow.surface", "COBBLESTONE");
        return yaml;
    }

    // ---- validateMaterialSettings ---------------------------------------------------------

    @Test
    void shippedMaterialsBlockValidatesWithNoMaterialWarning() {
        CapturingLogger capture = new CapturingLogger("shippedMaterials");
        YamlConfiguration yaml = shippedMaterialsYaml();
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        boolean result = validator.validateConfig();

        assertTrue(result, "validateConfig should report success");
        assertFalse(capture.anyMessageContains("palette"),
                "no message should reference the removed palette key; got: " + capture.messages);
        assertFalse(capture.anyMessageContains("material"),
                "a correctly-populated materials block must not produce a material warning; got: "
                        + capture.messages);
    }

    @Test
    void missingMaterialsSectionIsSeededWithTheShippedTierDefaults() {
        CapturingLogger capture = new CapturingLogger("missingMaterials");
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertEquals("CALCITE", yaml.getString("materials.deep.core"));
        assertEquals("AMETHYST_BLOCK", yaml.getString("materials.deep.middle"));
        assertEquals("BUDDING_AMETHYST", yaml.getString("materials.deep.surface"));
        assertEquals("PRISMARINE", yaml.getString("materials.medium-deep.core"));
        assertEquals("COPPER_BLOCK", yaml.getString("materials.medium.core"));
        assertEquals("OXIDIZED_COPPER", yaml.getString("materials.medium.surface"));
        assertEquals("STONE", yaml.getString("materials.shallow.core"));
        assertEquals("COBBLESTONE", yaml.getString("materials.shallow.surface"));
    }

    @Test
    void missingSingleTierWithinMaterialsIsAlsoSeeded() {
        CapturingLogger capture = new CapturingLogger("missingTier");
        YamlConfiguration yaml = shippedMaterialsYaml();
        yaml.set("materials.shallow", null);
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertEquals("STONE", yaml.getString("materials.shallow.core"));
        assertEquals("COBBLESTONE", yaml.getString("materials.shallow.surface"));
    }

    // ---- validateGenerationSettings: new height keys ---------------------------------------

    @Test
    void outOfRangeFloorYIsCorrectedToDefault() {
        CapturingLogger capture = new CapturingLogger("floorY");
        YamlConfiguration yaml = shippedMaterialsYaml();
        yaml.set("generation.floor-y", 500); // above the -64..300 sane range
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertEquals(135, yaml.getInt("generation.floor-y"));
    }

    @Test
    void inRangeFloorYIsLeftUntouched() {
        CapturingLogger capture = new CapturingLogger("floorYInRange");
        YamlConfiguration yaml = shippedMaterialsYaml();
        yaml.set("generation.floor-y", 140);
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertEquals(140, yaml.getInt("generation.floor-y"));
    }

    @Test
    void outOfRangeSurfaceBaseYIsCorrectedToDefault() {
        CapturingLogger capture = new CapturingLogger("surfaceBaseY");
        YamlConfiguration yaml = shippedMaterialsYaml();
        yaml.set("generation.surface-base-y", -100); // below the -64..300 sane range
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertEquals(153, yaml.getInt("generation.surface-base-y"));
    }

    @Test
    void outOfRangeReliefAmplitudeIsCorrectedToDefault() {
        CapturingLogger capture = new CapturingLogger("reliefAmplitude");
        YamlConfiguration yaml = shippedMaterialsYaml();
        yaml.set("generation.relief-amplitude", -1); // below the 0..64 sane range
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertEquals(12, yaml.getInt("generation.relief-amplitude"));
    }

    @Test
    void missingHeightKeysDefaultWithoutWarning() {
        CapturingLogger capture = new CapturingLogger("missingHeightKeys");
        YamlConfiguration yaml = shippedMaterialsYaml();
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        // Absent-but-valid keys are read through their defaults (getInt(path, default)) rather
        // than being written back, mirroring the existing generation.* validation pattern.
        assertEquals(135, yaml.getInt("generation.floor-y", 135));
        assertEquals(153, yaml.getInt("generation.surface-base-y", 153));
        assertEquals(12, yaml.getInt("generation.relief-amplitude", 12));
        assertFalse(capture.anyMessageContains("floor-y"));
        assertFalse(capture.anyMessageContains("surface-base-y"));
        assertFalse(capture.anyMessageContains("relief-amplitude"));
    }

    // ---- validatePermissionSettings ---------------------------------------------------------

    @Test
    void defaultPermissionsDoNotIncludeTheRemovedAdminNode() {
        CapturingLogger capture = new CapturingLogger("permissions");
        YamlConfiguration yaml = shippedMaterialsYaml();
        ConfigValidator validator = new ConfigValidator(yaml, capture.logger);

        validator.validateConfig();

        assertFalse(yaml.contains("permissions.admin"), "geobrot.admin was dropped in M2");
        assertFalse(capture.anyMessageContains("geobrot.admin"));
        assertEquals("geobrot.create", yaml.getString("permissions.create_world"));
        assertEquals("geobrot.teleport", yaml.getString("permissions.teleport"));
    }
}
