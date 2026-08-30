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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GeodePalette}: the immutable holder of the resolved geode {@link Material}
 * tiers, tier-boundary classification, and within-tier sub-layering.
 */
class GeodePaletteTest {

    // ---- tierFor boundaries ------------------------------------------------------------

    @Test
    void escapeTime100IsDeep() {
        assertEquals(GeodePalette.TIER_DEEP, GeodePalette.tierFor(100));
    }

    @Test
    void escapeTime71IsDeep() {
        assertEquals(GeodePalette.TIER_DEEP, GeodePalette.tierFor(71));
    }

    @Test
    void escapeTime70IsMediumDeep() {
        assertEquals(GeodePalette.TIER_MEDIUM_DEEP, GeodePalette.tierFor(70));
    }

    @Test
    void escapeTime50IsMediumDeep() {
        assertEquals(GeodePalette.TIER_MEDIUM_DEEP, GeodePalette.tierFor(50));
    }

    @Test
    void escapeTime49IsMedium() {
        assertEquals(GeodePalette.TIER_MEDIUM, GeodePalette.tierFor(49));
    }

    @Test
    void escapeTime30IsMedium() {
        assertEquals(GeodePalette.TIER_MEDIUM, GeodePalette.tierFor(30));
    }

    @Test
    void escapeTime29IsShallow() {
        assertEquals(GeodePalette.TIER_SHALLOW, GeodePalette.tierFor(29));
    }

    @Test
    void escapeTime0IsShallow() {
        assertEquals(GeodePalette.TIER_SHALLOW, GeodePalette.tierFor(0));
    }

    // ---- fromConfig ---------------------------------------------------------------------

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

    @Test
    void fromConfigResolvesTheShippedMaterialsBlock() {
        GeodePalette palette = GeodePalette.fromConfig(shippedMaterialsYaml());

        assertEquals(Material.CALCITE, palette.coreFor(GeodePalette.TIER_DEEP));
        assertEquals(Material.AMETHYST_BLOCK, palette.middleFor(GeodePalette.TIER_DEEP));
        assertEquals(Material.BUDDING_AMETHYST, palette.surfaceFor(GeodePalette.TIER_DEEP));

        assertEquals(Material.PRISMARINE, palette.coreFor(GeodePalette.TIER_MEDIUM_DEEP));
        assertEquals(Material.PRISMARINE_BRICKS, palette.middleFor(GeodePalette.TIER_MEDIUM_DEEP));
        assertEquals(Material.DARK_PRISMARINE, palette.surfaceFor(GeodePalette.TIER_MEDIUM_DEEP));

        assertEquals(Material.COPPER_BLOCK, palette.coreFor(GeodePalette.TIER_MEDIUM));
        assertNull(palette.middleFor(GeodePalette.TIER_MEDIUM));
        assertEquals(Material.OXIDIZED_COPPER, palette.surfaceFor(GeodePalette.TIER_MEDIUM));

        assertEquals(Material.STONE, palette.coreFor(GeodePalette.TIER_SHALLOW));
        assertNull(palette.middleFor(GeodePalette.TIER_SHALLOW));
        assertEquals(Material.COBBLESTONE, palette.surfaceFor(GeodePalette.TIER_SHALLOW));
    }

    @Test
    void fromConfigOnAnEmptyConfigYieldsTheTableDefaults() {
        GeodePalette palette = GeodePalette.fromConfig(new YamlConfiguration());

        assertEquals(Material.CALCITE, palette.coreFor(GeodePalette.TIER_DEEP));
        assertEquals(Material.AMETHYST_BLOCK, palette.middleFor(GeodePalette.TIER_DEEP));
        assertEquals(Material.BUDDING_AMETHYST, palette.surfaceFor(GeodePalette.TIER_DEEP));

        assertEquals(Material.PRISMARINE, palette.coreFor(GeodePalette.TIER_MEDIUM_DEEP));
        assertEquals(Material.PRISMARINE_BRICKS, palette.middleFor(GeodePalette.TIER_MEDIUM_DEEP));
        assertEquals(Material.DARK_PRISMARINE, palette.surfaceFor(GeodePalette.TIER_MEDIUM_DEEP));

        assertEquals(Material.COPPER_BLOCK, palette.coreFor(GeodePalette.TIER_MEDIUM));
        assertNull(palette.middleFor(GeodePalette.TIER_MEDIUM));
        assertEquals(Material.OXIDIZED_COPPER, palette.surfaceFor(GeodePalette.TIER_MEDIUM));

        assertEquals(Material.STONE, palette.coreFor(GeodePalette.TIER_SHALLOW));
        assertNull(palette.middleFor(GeodePalette.TIER_SHALLOW));
        assertEquals(Material.COBBLESTONE, palette.surfaceFor(GeodePalette.TIER_SHALLOW));
    }

    @Test
    void fromConfigFallsBackToDefaultWhenAKeyIsMissingOrUnparseable() {
        YamlConfiguration yaml = shippedMaterialsYaml();
        yaml.set("materials.deep.core", "NOT_A_REAL_MATERIAL");
        yaml.set("materials.medium.surface", null);

        GeodePalette palette = GeodePalette.fromConfig(yaml);

        assertEquals(Material.CALCITE, palette.coreFor(GeodePalette.TIER_DEEP),
                "unparseable material name must fall back to the table default");
        assertEquals(Material.OXIDIZED_COPPER, palette.surfaceFor(GeodePalette.TIER_MEDIUM),
                "missing key must fall back to the table default");
    }

    // ---- geodeMaterialAt sub-layering -----------------------------------------------------

    @Test
    void deepTierSurfaceBandUsesSurfaceMaterial() {
        GeodePalette palette = GeodePalette.fromConfig(new YamlConfiguration());

        // escapeTime 100 -> deep tier; band height 9; depthFromTop 0: 3*0 < 9 -> surface
        assertEquals(Material.BUDDING_AMETHYST, palette.geodeMaterialAt(100, 0, 9));
    }

    @Test
    void deepTierMiddleBandUsesMiddleMaterial() {
        GeodePalette palette = GeodePalette.fromConfig(new YamlConfiguration());

        // depthFromTop 4: 3*4=12, not < 9, but < 2*9=18 -> middle
        assertEquals(Material.AMETHYST_BLOCK, palette.geodeMaterialAt(100, 4, 9));
    }

    @Test
    void deepTierCoreBandUsesCoreMaterial() {
        GeodePalette palette = GeodePalette.fromConfig(new YamlConfiguration());

        // depthFromTop 8: 3*8=24, not < 9 and not < 18 -> core
        assertEquals(Material.CALCITE, palette.geodeMaterialAt(100, 8, 9));
    }

    @Test
    void shallowTierMiddleBandFallsThroughToCoreWhenNoMiddleIsConfigured() {
        GeodePalette palette = GeodePalette.fromConfig(new YamlConfiguration());

        // escapeTime 0 -> shallow tier (no middle material); depthFromTop 4 lands in the
        // middle third, which must fall through to core (STONE) rather than NPE or surface.
        assertEquals(Material.STONE, palette.geodeMaterialAt(0, 4, 9));
    }
}
