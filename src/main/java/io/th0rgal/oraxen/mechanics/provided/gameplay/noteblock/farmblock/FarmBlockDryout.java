package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Bukkit;
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
        Bukkit.broadcastMessage(String.valueOf(block.getType()));

        if (blockLoc.clone().subtract(0, 1, 0).getBlock().getType() == Material.WATER) {
            Bukkit.broadcastMessage("is water below");
            NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
            customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
            return true;
        }

        for (int i = 1; i <= 9; i++) {
            Block nextBlock = blockLoc.getBlock();
            if (nextBlock.getType() == Material.WATER) {
                Bukkit.broadcastMessage("is water");
                NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                return true;
            } else if (nextBlock.getType() == Material.FARMLAND) {
                Farmland data = (Farmland) nextBlock.getBlockData();
                if (data.getMoisture() > 0) {
                    NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                    customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                    return true;
                }
            } else if (nextBlock.getType() == Material.NOTE_BLOCK &&
                    getNoteBlockMechanic(nextBlock).hasDryout() &&
                    getNoteBlockMechanic(nextBlock).getDryout().isMoistFarmBlock()) {
                Bukkit.broadcastMessage("is moistfarmblock");
                NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                return true;
            }
            blockLoc = blockLoc.add(0, 0, -1);
        }
        return false;
    }

    //Attempt at checking all blocks in a radius of 9 for water source
    // Could find all waterblocks in radius and check if it is connected to farmblock via farmblocks/farmland

        /*for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= 9; j++) {
                Block nextBlock = blockLoc.getBlock();
                if (nextBlock.getType() == Material.WATER) {
                    Bukkit.broadcastMessage("is water");
                    NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                    customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                    return true;
                } else if (nextBlock.getType() == Material.FARMLAND) {
                    Farmland data = (Farmland) nextBlock.getBlockData();
                    if (data.getMoisture() > 0) {
                        NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                        customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                        return true;
                    }
                } else if (nextBlock.getType() == Material.NOTE_BLOCK &&
                        getNoteBlockMechanic(nextBlock).hasDryout() &&
                        getNoteBlockMechanic(nextBlock).getDryout().isMoistFarmBlock()) {

                    NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                    customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                    return true;
                }

                else if (nextBlock.getType() == Material.FARMLAND ||
                        (nextBlock.getType() == Material.NOTE_BLOCK && getNoteBlockMechanic(nextBlock).getDryout().isFarmBlock())) {
                    Bukkit.broadcastMessage("is farmblock");
                    Location nextLoc = nextBlock.getLocation().clone();
                    for (int k = 1; k <= 9; k++) {
                        if (nextBlock.getType() == Material.WATER) {
                            Bukkit.broadcastMessage("is farmblock and water");
                            NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
                            customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
                            return true;
                        }
                        nextLoc = nextLoc.add(0, 0, -1);
                    }
                }

                blockLoc = blockLoc.add(0, 0, -1);
            }

            blockLoc = blockLoc.add(-1, 0, 9);
        }
        Bukkit.broadcastMessage("nothing");
        return false;
    }*/
}


