package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Placeable {

    private final boolean floor;
    private final boolean wall;
    private final boolean roof;
    private final Set<String> allowedBlocks;
    private final Set<String> disallowedBlocks;

    public Placeable(ConfigurationSection section) {
        boolean parsedFloor = true;
        boolean parsedWall = true;
        boolean parsedRoof = true;
        Set<String> parsedAllowedBlocks = new HashSet<>();
        Set<String> parsedDisallowedBlocks = new HashSet<>();

        ConfigurationSection placeableSection = section.getConfigurationSection("placeable");
        if (placeableSection != null) {
            parsedFloor = placeableSection.getBoolean("floor", true);
            parsedWall = placeableSection.getBoolean("wall", true);
            parsedRoof = placeableSection.getBoolean("roof", true);
            parsedAllowedBlocks.addAll(parseBlockList(placeableSection.get("allow")));
            parsedDisallowedBlocks.addAll(parseBlockList(placeableSection.get("disallow")));
        } else if (section.isList("placeable")) {
            for (Map<?, ?> entry : section.getMapList("placeable")) {
                if (entry.containsKey("floor")) parsedFloor = Boolean.parseBoolean(entry.get("floor").toString());
                if (entry.containsKey("wall")) parsedWall = Boolean.parseBoolean(entry.get("wall").toString());
                if (entry.containsKey("roof")) parsedRoof = Boolean.parseBoolean(entry.get("roof").toString());
                if (entry.containsKey("allow")) parsedAllowedBlocks.addAll(parseBlockList(entry.get("allow")));
                if (entry.containsKey("disallow")) parsedDisallowedBlocks.addAll(parseBlockList(entry.get("disallow")));
            }
        }

        floor = parsedFloor;
        wall = parsedWall;
        roof = parsedRoof;
        allowedBlocks = Set.copyOf(parsedAllowedBlocks);
        disallowedBlocks = Set.copyOf(parsedDisallowedBlocks);
    }

    public boolean canPlaceOn(BlockFace face) {
        return switch (face) {
            case UP -> floor;
            case DOWN -> roof;
            case NORTH, EAST, SOUTH, WEST -> wall;
            default -> true;
        };
    }

    public boolean canPlaceOn(BlockFace face, Block block) {
        return canPlaceOn(face) && canPlaceAgainst(block);
    }

    public boolean canPlaceAgainst(Block block) {
        return block == null || canPlaceAgainst(block.getType());
    }

    private boolean canPlaceAgainst(Material material) {
        if (!allowedBlocks.isEmpty() && !matches(allowedBlocks, material)) return false;
        return !matches(disallowedBlocks, material);
    }

    private boolean matches(Set<String> blocks, Material material) {
        if (blocks.isEmpty() || material == null) return false;

        String materialName = material.name().toLowerCase(Locale.ROOT);
        if (blocks.contains(materialName)) return true;

        NamespacedKey key = material.getKey();
        return blocks.contains(key.asString()) || blocks.contains(key.getKey());
    }

    private Set<String> parseBlockList(Object value) {
        Set<String> blocks = new HashSet<>();
        if (value instanceof List<?> list) {
            for (Object entry : list) addBlock(blocks, entry);
        } else {
            addBlock(blocks, value);
        }
        return blocks;
    }

    private void addBlock(Set<String> blocks, Object value) {
        if (value == null) return;
        String block = value.toString().trim().toLowerCase(Locale.ROOT);
        if (!block.isEmpty()) blocks.add(block);
    }
}
