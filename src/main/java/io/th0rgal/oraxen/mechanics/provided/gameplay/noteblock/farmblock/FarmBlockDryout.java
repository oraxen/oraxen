package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class FarmBlockDryout {

    private String farmBlock;
    private String moistFarmBlock;
    private int farmBlockDryoutTime;
    private final String id;

    public FarmBlockDryout(String itemID, ConfigurationSection farmblockSection) {
        id = itemID;
        farmBlock = farmblockSection.getString("farmBlockPath");
        moistFarmBlock = farmblockSection.getString("moistFarmBlockPath");
        farmBlockDryoutTime = farmblockSection.getInt("farmBlockDryOutTime", 50000);
    }

    public int getDelay() {
        return farmBlockDryoutTime;
    }

    public boolean isFarmBlock() {
        return (farmBlock != null || moistFarmBlock != null);
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

    public boolean isConnectedToWaterSource(Block block, PersistentDataContainer customBlockData) {
        Location blockLoc = block.getLocation();

        if (blockLoc.clone().subtract(0, 1, 0).getBlock().getType() == Material.WATER) {
            NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
            customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
            return true;
        }

        for (int i = 1; i <= 9; i++) {
            Block nextBlock = blockLoc.getBlock();
            if (nextBlock.getType() == Material.WATER) return true;
            else if (nextBlock.getType() == Material.FARMLAND && ((Farmland) nextBlock.getBlockData()).getMoisture() > 0) return true;
            else if (nextBlock.getType() == Material.NOTE_BLOCK &&
                    getNoteBlockMechanic(nextBlock).hasDryout() &&
                    getNoteBlockMechanic(nextBlock).getDryout().isMoistFarmBlock())
                return true;

            blockLoc = blockLoc.add(0, 0, -1);
        }
        return false;
    }
}


