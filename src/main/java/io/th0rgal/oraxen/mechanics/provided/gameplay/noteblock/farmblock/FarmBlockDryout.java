package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class FarmBlockDryout {
    private static final NamespacedKey FARMBLOCK_MOIST = new NamespacedKey(OraxenPlugin.get(), "farmblock_moist");
    private final String farmBlock;
    private final String moistFarmBlock;
    private final int farmBlockDryoutTime;
    private final String id;

    public FarmBlockDryout(String itemID, ConfigurationSection farmblockSection) {
        id = itemID;
        farmBlock = farmblockSection.getString("farmBlockPath");
        moistFarmBlock = farmblockSection.getString("moistFarmBlockPath");
        farmBlockDryoutTime = farmblockSection.getInt("farmBlockDryOutTime", 50000);
    }

    public int getDelay() {
        return getDryoutTime();
    }

    public boolean isFarmBlock() {
        return (farmBlock != null || moistFarmBlock != null);
    }

    public byte getMoistureLevel(Block block) {
        if (!isMoistFarmBlock()) return 0;
        return BlockHelpers.getPDC(block).getOrDefault(FARMBLOCK_MOIST, PersistentDataType.BYTE, (byte) 0);
    }

    public boolean isMoistFarmBlock() {
        return ((moistFarmBlock == null || moistFarmBlock.equals(id)) && farmBlock != null);
    }

    public String getFarmBlock() {
        return farmBlock;
    }

    public String getMoistFarmBlock() {
        return moistFarmBlock;
    }

    public int getDryoutTime() {
        return farmBlockDryoutTime;
    }

    private int getMoisture(Block block) {
        NoteBlockMechanic temp;
        return switch (block.getType()) {
            case WATER -> 5;
            case FARMLAND -> ((Farmland) block.getBlockData()).getMoisture() > 0 ? 1 : 0;
            case NOTE_BLOCK -> (temp = OraxenBlocks.getNoteBlockMechanic(block)) != null && temp.hasDryout() ?
                    (int) temp.getDryout() .getMoistureLevel(block) : 0;
            default -> 0;
        };
    }

    public boolean isConnectedToWaterSource(Block block, PersistentDataContainer pdc) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(block.getRelative(BlockFace.DOWN));
        for (int x = -1; x < 2; x++)
            for (int z = -1; z < 2; z++)
                if (x != 0 || z != 0)
                    blocks.add(block.getRelative(x, 0, z));

        int maxLevel = 0;
        for (Block x : blocks) {
            maxLevel = Math.max(maxLevel, getMoisture(x));
            if (maxLevel == 5)
                break;
        }

        int moistLevel = Math.max(0, maxLevel - 1);
        pdc.set(FarmBlockDryout.FARMBLOCK_MOIST, PersistentDataType.BYTE, (byte) moistLevel);
        return moistLevel > 0;
    }
}


