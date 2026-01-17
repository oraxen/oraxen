package io.th0rgal.oraxen.mechanics.provided.gameplay.shaped;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the different types of shaped block variants that can be customized.
 * Each type uses waxed copper blocks for custom items and non-waxed copper for vanilla.
 */
public enum ShapedBlockType {
    STAIR(
        new Material[]{
            Material.WAXED_CUT_COPPER_STAIRS,
            Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,
            Material.WAXED_WEATHERED_CUT_COPPER_STAIRS,
            Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS
        },
        new Material[]{
            Material.CUT_COPPER_STAIRS,
            Material.EXPOSED_CUT_COPPER_STAIRS,
            Material.WEATHERED_CUT_COPPER_STAIRS,
            Material.OXIDIZED_CUT_COPPER_STAIRS
        }
    ),
    SLAB(
        new Material[]{
            Material.WAXED_CUT_COPPER_SLAB,
            Material.WAXED_EXPOSED_CUT_COPPER_SLAB,
            Material.WAXED_WEATHERED_CUT_COPPER_SLAB,
            Material.WAXED_OXIDIZED_CUT_COPPER_SLAB
        },
        new Material[]{
            Material.CUT_COPPER_SLAB,
            Material.EXPOSED_CUT_COPPER_SLAB,
            Material.WEATHERED_CUT_COPPER_SLAB,
            Material.OXIDIZED_CUT_COPPER_SLAB
        }
    ),
    DOOR(
        getMaterialsIfAvailable(new String[]{
            "WAXED_COPPER_DOOR",
            "WAXED_EXPOSED_COPPER_DOOR",
            "WAXED_WEATHERED_COPPER_DOOR",
            "WAXED_OXIDIZED_COPPER_DOOR"
        }),
        getMaterialsIfAvailable(new String[]{
            "COPPER_DOOR",
            "EXPOSED_COPPER_DOOR",
            "WEATHERED_COPPER_DOOR",
            "OXIDIZED_COPPER_DOOR"
        })
    ),
    TRAPDOOR(
        getMaterialsIfAvailable(new String[]{
            "WAXED_COPPER_TRAPDOOR",
            "WAXED_EXPOSED_COPPER_TRAPDOOR",
            "WAXED_WEATHERED_COPPER_TRAPDOOR",
            "WAXED_OXIDIZED_COPPER_TRAPDOOR"
        }),
        getMaterialsIfAvailable(new String[]{
            "COPPER_TRAPDOOR",
            "EXPOSED_COPPER_TRAPDOOR",
            "WEATHERED_COPPER_TRAPDOOR",
            "OXIDIZED_COPPER_TRAPDOOR"
        })
    ),
    GRATE(
        getMaterialsIfAvailable(new String[]{
            "WAXED_COPPER_GRATE",
            "WAXED_EXPOSED_COPPER_GRATE",
            "WAXED_WEATHERED_COPPER_GRATE",
            "WAXED_OXIDIZED_COPPER_GRATE"
        }),
        getMaterialsIfAvailable(new String[]{
            "COPPER_GRATE",
            "EXPOSED_COPPER_GRATE",
            "WEATHERED_COPPER_GRATE",
            "OXIDIZED_COPPER_GRATE"
        })
    ),
    BULB(
        getMaterialsIfAvailable(new String[]{
            "WAXED_COPPER_BULB",
            "WAXED_EXPOSED_COPPER_BULB",
            "WAXED_WEATHERED_COPPER_BULB",
            "WAXED_OXIDIZED_COPPER_BULB"
        }),
        getMaterialsIfAvailable(new String[]{
            "COPPER_BULB",
            "EXPOSED_COPPER_BULB",
            "WEATHERED_COPPER_BULB",
            "OXIDIZED_COPPER_BULB"
        })
    );

    private final Material[] waxedMaterials;
    private final Material[] vanillaMaterials;

    // Static lookup maps for fast conversion
    private static final Map<Material, Material> WAXED_TO_VANILLA = new HashMap<>();
    private static final Map<Material, Material> VANILLA_TO_WAXED = new HashMap<>();
    private static final Set<Material> ALL_WAXED = new HashSet<>();
    private static final Set<Material> ALL_VANILLA = new HashSet<>();

    /**
     * Helper method to safely get materials by name, returning empty array if they don't exist.
     * This allows the enum to work on older Minecraft versions that don't have certain materials.
     */
    private static Material[] getMaterialsIfAvailable(String[] materialNames) {
        Material[] materials = new Material[materialNames.length];
        for (int i = 0; i < materialNames.length; i++) {
            try {
                materials[i] = Material.valueOf(materialNames[i]);
            } catch (IllegalArgumentException e) {
                // Material doesn't exist in this version, return empty array
                return new Material[0];
            }
        }
        return materials;
    }

    static {
        for (ShapedBlockType type : values()) {
            // Skip types that have no materials (not available in this version)
            if (type.waxedMaterials.length == 0 || type.vanillaMaterials.length == 0) {
                if (Settings.DEBUG.toBool()) {
                    Logs.logInfo("ShapedBlockType " + type.name() + " is not available in this Minecraft version - skipping");
                }
                continue;
            }
            
            for (int i = 0; i < type.waxedMaterials.length; i++) {
                Material waxed = type.waxedMaterials[i];
                Material vanilla = type.vanillaMaterials[i];
                if (waxed != null && vanilla != null) {
                    WAXED_TO_VANILLA.put(waxed, vanilla);
                    VANILLA_TO_WAXED.put(vanilla, waxed);
                    ALL_WAXED.add(waxed);
                    ALL_VANILLA.add(vanilla);
                }
            }
        }
    }

    ShapedBlockType(Material[] waxedMaterials, Material[] vanillaMaterials) {
        this.waxedMaterials = waxedMaterials != null ? waxedMaterials : new Material[0];
        this.vanillaMaterials = vanillaMaterials != null ? vanillaMaterials : new Material[0];
    }

    /**
     * Get the waxed material for a specific custom variation (1-4).
     * Used for custom shaped blocks.
     */
    public Material getMaterial(int customVariation) {
        if (customVariation < 1 || customVariation > 4) {
            throw new IllegalArgumentException("custom_variation must be between 1 and 4, got: " + customVariation);
        }
        if (waxedMaterials.length == 0) {
            throw new UnsupportedOperationException("Block type " + this.name() + " is not available in this Minecraft version");
        }
        return waxedMaterials[customVariation - 1];
    }

    /**
     * Get all waxed materials for this block type (used for custom blocks)
     */
    public Material[] getMaterials() {
        return waxedMaterials;
    }

    /**
     * Get the vanilla (non-waxed) material for a specific variation (1-4).
     * Used for converting vanilla waxed blocks to non-waxed.
     */
    public Material getVanillaMaterial(int variation) {
        if (variation < 1 || variation > 4) {
            throw new IllegalArgumentException("variation must be between 1 and 4, got: " + variation);
        }
        if (vanillaMaterials.length == 0) {
            throw new UnsupportedOperationException("Block type " + this.name() + " is not available in this Minecraft version");
        }
        return vanillaMaterials[variation - 1];
    }

    /**
     * Get all vanilla (non-waxed) materials for this block type
     */
    public Material[] getVanillaMaterials() {
        return vanillaMaterials;
    }

    /**
     * Check if this block type is available in the current Minecraft version
     */
    public boolean isAvailable() {
        return waxedMaterials.length > 0 && vanillaMaterials.length > 0;
    }

    /**
     * Check if a material is a waxed copper material for this type
     */
    public boolean contains(Material material) {
        for (Material m : waxedMaterials) {
            if (m == material) return true;
        }
        return false;
    }

    /**
     * Check if a material is a vanilla (non-waxed) copper material for this type
     */
    public boolean containsVanilla(Material material) {
        for (Material m : vanillaMaterials) {
            if (m == material) return true;
        }
        return false;
    }

    /**
     * Get the custom variation for a waxed material (1-4)
     * @return The variation number, or -1 if not found
     */
    public int getVariation(Material material) {
        for (int i = 0; i < waxedMaterials.length; i++) {
            if (waxedMaterials[i] == material) return i + 1;
        }
        return -1;
    }

    /**
     * Find the block type for a given waxed material
     */
    public static ShapedBlockType fromMaterial(Material material) {
        for (ShapedBlockType type : values()) {
            if (type.contains(material)) return type;
        }
        return null;
    }

    /**
     * Find the block type for a given vanilla (non-waxed) material
     */
    public static ShapedBlockType fromVanillaMaterial(Material material) {
        for (ShapedBlockType type : values()) {
            if (type.containsVanilla(material)) return type;
        }
        return null;
    }

    /**
     * Convert a waxed copper material to its non-waxed equivalent.
     * Used when vanilla waxed copper is placed - convert to non-waxed and mark.
     */
    public static Material toVanilla(Material waxed) {
        return WAXED_TO_VANILLA.get(waxed);
    }

    /**
     * Convert a non-waxed copper material to its waxed equivalent.
     */
    public static Material toWaxed(Material vanilla) {
        return VANILLA_TO_WAXED.get(vanilla);
    }

    /**
     * Check if a material is any waxed copper material used by this system
     */
    public static boolean isWaxedCopper(Material material) {
        return ALL_WAXED.contains(material);
    }

    /**
     * Check if a material is any vanilla (non-waxed) copper material used by this system
     */
    public static boolean isVanillaCopper(Material material) {
        return ALL_VANILLA.contains(material);
    }

    /**
     * Get all waxed copper materials used by this system
     */
    public static Set<Material> getAllWaxedMaterials() {
        return ALL_WAXED;
    }

    /**
     * Get all vanilla (non-waxed) copper materials used by this system
     */
    public static Set<Material> getAllVanillaMaterials() {
        return ALL_VANILLA;
    }
}
