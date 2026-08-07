package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.apache.commons.lang3.Range;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
        parentBlock = directionalSection.getString("parent_block");
        directionalType = DirectionalType.valueOf(directionalSection.getString("directional_type", "LOG"));
        yBlock = directionalSection.getString("y_block");
        xBlock = directionalSection.getString("x_block");
        zBlock = directionalSection.getString("z_block");
        northBlock = directionalSection.getString("north_block");
        southBlock = directionalSection.getString("south_block");
        eastBlock = directionalSection.getString("east_block");
        westBlock = directionalSection.getString("west_block");
        upBlock = directionalSection.getString("up_block");
        downBlock = directionalSection.getString("down_block");

    }
    public boolean isParentBlock() { return parentBlock == null; }
    public String getParentBlock() { return parentBlock; }
    public NoteBlockMechanic getParentMechanic() {
        if (parentBlock == null) return null;
        else return (NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(parentBlock);
    }

    public DirectionalType getDirectionalType() { return directionalType; }
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

    public int getDirectionVariation(BlockFace face, Player player) {
        if (isLog()) {
            switch (face) {
                case NORTH: case SOUTH: return getDirectionVariation(xBlock);
                case EAST: case WEST: return getDirectionVariation(zBlock);
                case UP: case DOWN: return getDirectionVariation(yBlock);
            }
        } else {
            switch (getRelativeFacing(player)) {
                case NORTH: return getDirectionVariation(northBlock);
                case SOUTH: return getDirectionVariation(southBlock);
                case EAST: return getDirectionVariation(eastBlock);
                case WEST: return getDirectionVariation(westBlock);
                case UP: return getDirectionVariation(upBlock);
                case DOWN: return getDirectionVariation(downBlock);
            }
        }
        return 0;
    }

    private BlockFace getRelativeFacing(Player player) {
        double yaw = player.getLocation().getYaw();
        double pitch = player.getLocation().getPitch();
        BlockFace face = BlockFace.SELF;
        if (!isLog()) {
            if (Range.between(0.0, 45.0).contains(yaw) || yaw >= 315.0 || yaw >= -45.0 && yaw <= 0.0 || yaw <= -315.0)
                face = BlockFace.NORTH;
            else if (Range.between(45.0, 135.0).contains(yaw) || Range.between(-315.0, -225.0).contains(yaw))
                face = BlockFace.EAST;
            else if (Range.between(135.0, 225.0).contains(yaw) || Range.between(-225.0, -135.0).contains(yaw))
                face = BlockFace.SOUTH;
            else if (Range.between(225.0, 315.0).contains(yaw) || Range.between(-135.0, -45.0).contains(yaw))
                face = BlockFace.WEST;

            if (isDropper()) face = (pitch <= -45.0) ? BlockFace.DOWN : (pitch >= 45.0) ? BlockFace.UP : face;
        }
        return face;
    }

    private int getDirectionVariation(String id) {
        return ((NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id)).getCustomVariation();
    }

    public String getDirectionalModel(NoteBlockMechanic mechanic) {
        return mechanic.getSection().getString("model");
    }
}
