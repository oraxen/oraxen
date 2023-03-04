package io.th0rgal.oraxen.utils.limitedplacing;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LimitedPlacing {
    private final LimitedPlacingType type;
    private final List<String> blockTypes;
    private final List<String> blockTags;
    private final List<String> oraxenBlocks;
    private final boolean floor;
    private final boolean roof;
    private final boolean wall;

    public LimitedPlacing(ConfigurationSection section) {
        floor = section.getBoolean("floor", true);
        roof = section.getBoolean("roof", true);
        wall = section.getBoolean("wall", true);
        type = LimitedPlacingType.valueOf(section.getString("type", "DENY"));
        blockTypes = section.getStringList("block_types");
        blockTags = section.getStringList("block_tags");
        oraxenBlocks = section.getStringList("oraxen_blocks");
    }

    public LimitedPlacingType getType() { return type; }

    public List<Material> getLimitedBlocks() {
        List<Material> blocks = new ArrayList<>();
        for (String blockType : blockTypes) {
            blocks.add(Material.getMaterial(blockType));
        }
        return blocks.stream().filter(Objects::nonNull).toList();
    }

    public boolean isNotPlacableOn(Block blockBelow, BlockFace blockFace) {
        return !switch (blockFace) {
            case UP -> floor;
            case DOWN -> roof;
            default -> wall || blockBelow.getType().isSolid();
        };
    }

    public List<String> getLimitedOraxenBlockIds() {
        List<String> ids = new ArrayList<>();
        for (String blockId : oraxenBlocks) {
            if (!OraxenItems.getItems().contains(OraxenItems.getItemById(blockId))) continue;
            ids.add(blockId);
        }
        return ids;
    }

    public boolean checkLimitedMechanic(Block block) {
        String oraxenId = checkIfOraxenItem(block);
        if (oraxenId == null) {
            if (getLimitedBlocks().contains(block.getType())) {
                return true;
            }
            for (Tag<Material> tag : getLimitedTags()) {
                if (tag.isTagged(block.getType())) {
                    return true;
                }
            }
        }
        List<String> limitedOraxenBlockIds = getLimitedOraxenBlockIds();
        return (oraxenId != null && !limitedOraxenBlockIds.isEmpty() && limitedOraxenBlockIds.contains(oraxenId));
    }

    private String checkIfOraxenItem(Block block) {

        return switch (block.getType()) {
            case NOTE_BLOCK, TRIPWIRE:
                Mechanic mechanic = OraxenBlocks.getOraxenBlock(block.getBlockData());
                if (mechanic == null) yield null;
                else yield mechanic.getItemID();
            case BARRIER:
                FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (furnitureMechanic != null) yield furnitureMechanic.getItemID();
                else yield null;
            case MUSHROOM_STEM:
                BlockMechanic blockMechanic = OraxenBlocks.getBlockMechanic(block);
                if (blockMechanic != null) yield blockMechanic.getItemID();
                else yield null;
            default: yield null;
        };
    }

    public List<Tag<Material>> getLimitedTags() {
        List<Tag<Material>> blocks = new ArrayList<>();
        for (String blockTag : blockTags) {
            blocks.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(blockTag), Material.class));
        }
        return blocks;
    }

    public enum LimitedPlacingType {
        ALLOW, DENY
    }

    public boolean isFloor() { return floor; }
    public boolean isRoof() {
        return roof;
    }
    public boolean isWall() {
        return wall;
    }
}
