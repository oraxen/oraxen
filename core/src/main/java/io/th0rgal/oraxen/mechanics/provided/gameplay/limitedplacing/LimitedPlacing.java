package io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LimitedPlacing {
    private final LimitedPlacingType type;
    private final List<Material> blockTypes;
    private final Set<Tag<Material>> blockTags;
    private final List<String> oraxenBlocks;
    private final boolean floor;
    private final boolean roof;
    private final boolean wall;
    private final RadiusLimitation radiusLimitation;

    public static final class RadiusLimitation {
        private final int radius;
        private final int amount;

        public RadiusLimitation(ConfigurationSection section) {
            radius = section.getInt("radius", -1);
            amount = section.getInt("amount", -1);
        }

        public int getRadius() {
            return radius;
        }

        public int getAmount() {
            return amount;
        }
    }

    public LimitedPlacing(ConfigurationSection section) {
        floor = section.getBoolean("floor", true);
        roof = section.getBoolean("roof", true);
        wall = section.getBoolean("wall", true);
        type = LimitedPlacingType.valueOf(section.getString("type", "DENY"));
        blockTypes = getLimitedBlockMaterials(section.getStringList("block_types"));
        blockTags = getLimitedBlockTags(section.getStringList("block_tags"));
        oraxenBlocks =  getLimitedOraxenBlocks(section.getStringList("oraxen_blocks"));

        ConfigurationSection radiusSection = section.getConfigurationSection("radius_limitation");
        radiusLimitation = radiusSection != null ? new RadiusLimitation(radiusSection) : null;
    }

    public boolean isRadiusLimited() {
        return radiusLimitation != null && radiusLimitation.getRadius() != -1 && radiusLimitation.getAmount() != -1;
    }

    public RadiusLimitation getRadiusLimitation() {
        return radiusLimitation;
    }

    private List<Material> getLimitedBlockMaterials(List<String> list) {
        return list.stream().map(Material::getMaterial).filter(Objects::nonNull).toList();
    }

    private List<String> getLimitedOraxenBlocks(List<String> list) {
        return list.stream().filter(e -> OraxenBlocks.isOraxenBlock(e) || OraxenFurniture.isFurniture(e)).toList();
    }

    private Set<Tag<Material>> getLimitedBlockTags(List<String> list) {
        Set<Tag<Material>> tags = new HashSet<>();
        for (String string : list) {
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(string), Material.class);
            if (tag != null) tags.add(tag);
        }
        return tags;
    }

    public LimitedPlacingType getType() { return type; }

    public boolean isNotPlacableOn(Block block, BlockFace blockFace) {
        Block placedBlock = block.getRelative(blockFace);
        Block blockBelow = placedBlock.getRelative(BlockFace.DOWN);
        Block blockAbove = placedBlock.getRelative(BlockFace.UP);

        if (wall && block.getType().isSolid() && blockFace.getModY() == 0) return false;
        if (floor && (blockFace == BlockFace.UP || blockBelow.getType().isSolid())) return false;
        if (roof && blockFace == BlockFace.DOWN) return false;
        return !roof || !blockAbove.getType().isSolid();
    }

    public List<Material> getLimitedBlocks() {
        return blockTypes;
    }

    public List<String> getLimitedOraxenBlockIds() {
        return oraxenBlocks;
    }

    public Set<Tag<Material>> getLimitedTags() {
        return blockTags;
    }

    public boolean checkLimitedMechanic(Block block) {
        if (blockTypes.isEmpty() && blockTags.isEmpty() && oraxenBlocks.isEmpty()) return type == LimitedPlacingType.ALLOW;
        String oraxenId = checkIfOraxenItem(block);
        if (oraxenId == null) {
            if (blockTypes.contains(block.getType())) return true;
            for (Tag<Material> tag : blockTags) {
                if (tag.isTagged(block.getType())) return true;
            }
        }

        return (oraxenId != null && !oraxenBlocks.isEmpty() && oraxenBlocks.contains(oraxenId));
    }

    private String checkIfOraxenItem(Block block) {

        return switch (block.getType()) {
            case NOTE_BLOCK, TRIPWIRE -> {
                Mechanic mechanic = OraxenBlocks.getOraxenBlock(block.getBlockData());
                if (mechanic == null) yield null;
                else yield mechanic.getItemID();
            }
            case BARRIER -> {
                FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (furnitureMechanic != null) yield furnitureMechanic.getItemID();
                else yield null;
            }
            case MUSHROOM_STEM -> {
                BlockMechanic blockMechanic = OraxenBlocks.getBlockMechanic(block);
                if (blockMechanic != null) yield blockMechanic.getItemID();
                else yield null;
            }
            default -> null;
        };
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
