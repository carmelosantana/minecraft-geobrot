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

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable holder of the resolved geode {@link Material} tiers, plus the escape-time tier
 * classification and within-tier sub-layering that pick a material for a given block.
 *
 * <p>There are four escape-time tiers - deep, medium-deep, medium, and shallow - each carrying
 * a core and surface material, and (for the two deepest tiers only) a middle material. Owns
 * both the locked default palette and the {@code materials.*} config parsing, so the generator
 * only ever asks "what material here", never touches {@link FileConfiguration} directly.
 */
public final class GeodePalette {

    /** Escape-time strictly greater than 70. */
    public static final int TIER_DEEP = 0;
    /** Escape-time in [50, 70]. */
    public static final int TIER_MEDIUM_DEEP = 1;
    /** Escape-time in [30, 49]. */
    public static final int TIER_MEDIUM = 2;
    /** Escape-time strictly less than 30. */
    public static final int TIER_SHALLOW = 3;

    private static final int TIER_COUNT = 4;

    /** One resolved {core, middle (nullable), surface} triple for a tier. */
    private record TierMaterials(Material core, Material middle, Material surface) {
    }

    private static final TierMaterials[] DEFAULTS = {
        new TierMaterials(Material.CALCITE, Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST),
        new TierMaterials(Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE),
        new TierMaterials(Material.COPPER_BLOCK, null, Material.OXIDIZED_COPPER),
        new TierMaterials(Material.STONE, null, Material.COBBLESTONE),
    };

    private static final String[] CONFIG_KEYS = {"deep", "medium-deep", "medium", "shallow"};

    private final TierMaterials[] tiers;

    private GeodePalette(TierMaterials[] tiers) {
        this.tiers = tiers;
    }

    /**
     * Resolve the palette from {@code materials.deep.{core,middle,surface}},
     * {@code materials.medium-deep.{core,middle,surface}}, {@code materials.medium.{core,surface}},
     * and {@code materials.shallow.{core,surface}}. Each material name is parsed with
     * {@link Material#matchMaterial(String)}; a missing or unparseable key falls back to the
     * table default for that slot (not an error - nothing is logged). Medium and shallow have
     * no middle material in config; their middle slot is always {@code null}.
     */
    public static GeodePalette fromConfig(FileConfiguration config) {
        TierMaterials[] resolved = new TierMaterials[TIER_COUNT];
        for (int tier = 0; tier < TIER_COUNT; tier++) {
            TierMaterials fallback = DEFAULTS[tier];
            String prefix = "materials." + CONFIG_KEYS[tier] + ".";

            Material core = resolveMaterial(config, prefix + "core", fallback.core());
            Material surface = resolveMaterial(config, prefix + "surface", fallback.surface());
            Material middle = fallback.middle() == null
                    ? null
                    : resolveMaterial(config, prefix + "middle", fallback.middle());

            resolved[tier] = new TierMaterials(core, middle, surface);
        }
        return new GeodePalette(resolved);
    }

    private static Material resolveMaterial(FileConfiguration config, String path, Material fallback) {
        String name = config.getString(path);
        if (name == null) {
            return fallback;
        }
        Material matched = Material.matchMaterial(name);
        return matched != null ? matched : fallback;
    }

    /**
     * Classify an escape-time into one of the four tiers: {@code >70} deep, {@code 50..70}
     * medium-deep, {@code 30..49} medium, else shallow.
     */
    public static int tierFor(int escapeTime) {
        if (escapeTime > 70) {
            return TIER_DEEP;
        } else if (escapeTime >= 50) {
            return TIER_MEDIUM_DEEP;
        } else if (escapeTime >= 30) {
            return TIER_MEDIUM;
        } else {
            return TIER_SHALLOW;
        }
    }

    public Material coreFor(int tier) {
        return tiers[tier].core();
    }

    /** May be {@code null} - the medium and shallow tiers have no middle material. */
    public Material middleFor(int tier) {
        return tiers[tier].middle();
    }

    public Material surfaceFor(int tier) {
        return tiers[tier].surface();
    }

    /**
     * The material for one block within a geode band, applying the within-tier sub-layering
     * rule on top of the tier picked by {@code escapeTime}: the outer third of the band
     * ({@code 3 * depthFromTop < bandHeight}) is surface, the middle third
     * ({@code 3 * depthFromTop < 2 * bandHeight}) is middle-or-core (core when the tier has no
     * middle material), and the inner third is core.
     *
     * @param escapeTime the Mandelbrot escape-time at this column, selecting the tier
     * @param depthFromTop how many blocks below the top of the band this block sits
     * @param bandHeight the total height of the geode band at this column
     */
    public Material geodeMaterialAt(int escapeTime, int depthFromTop, int bandHeight) {
        int tier = tierFor(escapeTime);
        TierMaterials materials = tiers[tier];

        if (3 * depthFromTop < bandHeight) {
            return materials.surface();
        } else if (3 * depthFromTop < 2 * bandHeight) {
            return materials.middle() != null ? materials.middle() : materials.core();
        } else {
            return materials.core();
        }
    }
}
