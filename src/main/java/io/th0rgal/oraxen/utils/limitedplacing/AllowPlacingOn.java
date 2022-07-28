package io.th0rgal.oraxen.utils.limitedplacing;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicListener.getBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.FURNITURE_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener.getStringMechanic;

public class AllowPlacingOn {
    private final List<String> blockTypes;
    private final List<String> blockTags;
    private final List<String> oraxenBlocks;

    public AllowPlacingOn(ConfigurationSection section) {
        blockTypes = section.getStringList("block_types");
        blockTags = section.getStringList("block_tags");
        oraxenBlocks = section.getStringList("oraxen_blocks");
    }

    public List<Material> getAllowedBlocks() {
        List<Material> blocks = new ArrayList<>();
        for (String blockType : blockTypes) {
            blocks.add(Material.getMaterial(blockType));
        }
        return blocks;
    }

    public List<Tag<Material>> getAllowedTags() {
        List<Tag<Material>> blocks = new ArrayList<>();
        for (String blockTag : blockTags) {
            blocks.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(blockTag), Material.class));
        }
        return blocks;
    }

    public List<String> getAllowedOraxenBlockIds() {
        List<String> ids = new ArrayList<>();
        for (String blockId : oraxenBlocks) {
            if (!OraxenItems.getItems().contains(OraxenItems.getItemById(blockId))) continue;
            ids.add(blockId);
        }
        return ids;
    }

    public boolean checkAllowedMechanic(Block block) {
        String oraxenId = checkIfOraxenItem(block);
        if (oraxenId == null) {
            if (getAllowedBlocks().contains(block.getType())) {
                return true;
            }
            for (Tag<Material> tag : getAllowedTags()) {
                if (tag.isTagged(block.getType())) {
                    return true;
                }
            }
        }
        return (oraxenId != null && getAllowedOraxenBlockIds().contains(oraxenId));
    }

    private String checkIfOraxenItem(Block block) {
        return switch (block.getType()) {
            case NOTE_BLOCK -> getNoteBlockMechanic(block) != null ? getNoteBlockMechanic(block).getItemID() : null;
            case TRIPWIRE -> getStringMechanic(block) != null ? getStringMechanic(block).getItemID() : null;
            case BARRIER -> getFurnitureMechanic(block) != null ? getFurnitureMechanic(block).getItemID() : null;
            case MUSHROOM_STEM -> getBlockMechanic(block) != null ? getBlockMechanic(block).getItemID() : null;
            default -> null;
        };
    }

    public FurnitureMechanic getFurnitureMechanic(Block block) {
        if (block.getType() != Material.BARRIER) return null;
        final PersistentDataContainer customBlockData = new CustomBlockData(block, OraxenPlugin.get());
        final String mechanicID = customBlockData.get(FURNITURE_KEY, PersistentDataType.STRING);
        return (FurnitureMechanic) FurnitureFactory.getInstance().getMechanic(mechanicID);
    }
}
