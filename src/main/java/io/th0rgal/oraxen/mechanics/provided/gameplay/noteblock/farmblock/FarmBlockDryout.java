package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class FarmBlockDryout {

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

        if (block.getRelative(BlockFace.DOWN).getType() == Material.WATER) {
            NoteBlockMechanicFactory.setBlockModel(block, getMoistFarmBlock());
            customBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, 0);
            return true;
        }

        final List<Block> blocks = new ArrayList<>();
        for (int x = blockLoc.getBlockX() - 1; x <= blockLoc.getBlockX()
                + 1; x++)
            for (int z = blockLoc.getBlockZ() - 1; z <= blockLoc.getBlockZ()
                    + 1; z++) {
                Block b = Objects.requireNonNull(blockLoc.getWorld()).getBlockAt(x, blockLoc.getBlockY(), z);
                if (b.getType() == Material.WATER) blocks.add(b);
                else if (b.getType() == Material.FARMLAND && ((Farmland) b.getBlockData()).getMoisture() > 0)
                    blocks.add(b);
                else if (b.getType() == Material.NOTE_BLOCK &&
                        getNoteBlockMechanic(b).hasDryout() &&
                        getNoteBlockMechanic(b).getDryout().isMoistFarmBlock())
                    blocks.add(b);
            }
        return !blocks.isEmpty();
    }
}


