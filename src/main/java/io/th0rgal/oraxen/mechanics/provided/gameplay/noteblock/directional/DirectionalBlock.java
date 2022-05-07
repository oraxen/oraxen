package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional;

import org.bukkit.configuration.ConfigurationSection;

public class DirectionalBlock {
    private final String parentBlock;
    private final String yBlock;
    private final String xBlock;
    private final String zBlock;

    public DirectionalBlock(ConfigurationSection directionalSection) {
        parentBlock = directionalSection.getString("parentBlock");
        yBlock = directionalSection.getString("yBlock");
        xBlock = directionalSection.getString("xBlock");
        zBlock = directionalSection.getString("zBlock");

    }
    public boolean isParentBlock() { return parentBlock == null; }
    public String getParentBlock() { return parentBlock; }
    public String getYBlock() { return yBlock; }
    public String getXBlock() { return xBlock; }
    public String getZBlock() { return zBlock; }

}
