package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock;

import org.bukkit.configuration.ConfigurationSection;

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

    public int getDryoutTime() { return farmBlockDryoutTime; }
}


