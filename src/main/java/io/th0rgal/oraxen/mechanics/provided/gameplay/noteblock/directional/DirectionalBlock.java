package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class DirectionalBlock {
    private final String parentBlock;
    private final DirectionalType directionalType;
    // LOG type
    private final String yBlock;
    private final String xBlock;
    private final String zBlock;
    // FURNACE/DROPPER type
    private final String northBlock;
    private final String southBlock;
    private final String eastBlock;
    private final String westBlock;
    // DROPPER type only
    private final String upBlock;
    private final String downBlock;

    public DirectionalBlock(ConfigurationSection directionalSection) {
        parentBlock = directionalSection.getString("parentBlock");
        directionalType = DirectionalType.valueOf(directionalSection.getString("directionalType", "LOG"));
        yBlock = directionalSection.getString("yBlock");
        xBlock = directionalSection.getString("xBlock");
        zBlock = directionalSection.getString("zBlock");
        northBlock = directionalSection.getString("northBlock");
        southBlock = directionalSection.getString("southBlock");
        eastBlock = directionalSection.getString("eastBlock");
        westBlock = directionalSection.getString("westBlock");
        upBlock = directionalSection.getString("upBlock");
        downBlock = directionalSection.getString("downBlock");

    }
    public boolean isParentBlock() { return parentBlock == null; }
    public String getParentBlock() { return parentBlock; }

    public DirectionalType getDirectionalType() {
        return directionalType;
    }
    public boolean isLog() { return directionalType == DirectionalType.LOG; }
    public boolean isFurnace() { return directionalType == DirectionalType.FURNACE; }
    public boolean isDropper() { return directionalType == DirectionalType.DROPPER; }

    public String getYBlock() { return yBlock; }
    public String getXBlock() { return xBlock; }
    public String getZBlock() { return zBlock; }

    public String getNorthBlock() { return northBlock; }
    public String getSouthBlock() { return southBlock; }
    public String getEastBlock() { return eastBlock; }
    public String getWestBlock() { return westBlock; }

    public String getUpBlock() { return upBlock; }
    public String getDownBlock() { return downBlock; }

    public enum DirectionalType {
        LOG, FURNACE, DROPPER
    }

    public int getDirectionVariation(String id) {
        return ((NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id)).getCustomVariation();
    }
}
