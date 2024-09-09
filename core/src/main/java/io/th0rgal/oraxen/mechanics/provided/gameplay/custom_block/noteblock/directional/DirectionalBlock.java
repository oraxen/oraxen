package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.directional;

import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanicFactory;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.Range;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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

    @Nullable
    public NoteBlockMechanic getParentMechanic() {
        if (parentBlock == null) return null;
        else return NoteBlockMechanicFactory.get().getMechanic(parentBlock);
    }

    public DirectionalType type() { return directionalType; }
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

    @Nullable
    public NoteBlockMechanic directionMechanic(BlockFace face, Player player) {
        if (isLog()) {
            switch (face) {
                case NORTH: case SOUTH: return directionMechanic(xBlock);
                case EAST: case WEST: return directionMechanic(zBlock);
                case UP: case DOWN: return directionMechanic(yBlock);
            }
        } else {
            switch (relativeFacing(player)) {
                case NORTH: return directionMechanic(northBlock);
                case SOUTH: return directionMechanic(southBlock);
                case EAST: return directionMechanic(eastBlock);
                case WEST: return directionMechanic(westBlock);
                case UP: return directionMechanic(upBlock);
                case DOWN: return directionMechanic(downBlock);
            }
        }
        return null;
    }

    public int directionVariation(BlockFace face, Player player) {
        if (isLog()) {
            switch (face) {
                case NORTH: case SOUTH: return directionVariation(xBlock);
                case EAST: case WEST: return directionVariation(zBlock);
                case UP: case DOWN: return directionVariation(yBlock);
            }
        } else {
            switch (relativeFacing(player)) {
                case NORTH: return directionVariation(northBlock);
                case SOUTH: return directionVariation(southBlock);
                case EAST: return directionVariation(eastBlock);
                case WEST: return directionVariation(westBlock);
                case UP: return directionVariation(upBlock);
                case DOWN: return directionVariation(downBlock);
            }
        }
        return 0;
    }

    private BlockFace relativeFacing(Player player) {
        double yaw = player.getLocation().getYaw();
        double pitch = player.getLocation().getPitch();
        BlockFace face = BlockFace.SELF;
        if (!isLog()) {
            if (Range.of(0.0, 45.0).contains(yaw) || yaw >= 315.0 || yaw >= -45.0 && yaw <= 0.0 || yaw <= -315.0)
                face = BlockFace.NORTH;
            else if (Range.of(45.0, 135.0).contains(yaw) || Range.of(-315.0, -225.0).contains(yaw))
                face = BlockFace.EAST;
            else if (Range.of(135.0, 225.0).contains(yaw) || Range.of(-225.0, -135.0).contains(yaw))
                face = BlockFace.SOUTH;
            else if (Range.of(225.0, 315.0).contains(yaw) || Range.of(-135.0, -45.0).contains(yaw))
                face = BlockFace.WEST;

            if (isDropper()) face = (pitch <= -45.0) ? BlockFace.DOWN : (pitch >= 45.0) ? BlockFace.UP : face;
        }
        return face;
    }

    private NoteBlockMechanic directionMechanic(String itemId) {
        return NoteBlockMechanicFactory.get().getMechanic(itemId);
    }

    private int directionVariation(String itemId) {
        return NoteBlockMechanicFactory.get().getMechanic(itemId).customVariation();
    }

    public Key directionalModel(NoteBlockMechanic mechanic) {
        return mechanic.model();
    }

    public boolean anyMatch(String itemId) {
        if (Objects.equals(xBlock, itemId)) return true;
        if (Objects.equals(zBlock, itemId)) return true;
        if (Objects.equals(yBlock, itemId)) return true;
        if (Objects.equals(upBlock, itemId)) return true;
        if (Objects.equals(downBlock, itemId)) return true;
        if (Objects.equals(westBlock, itemId)) return true;
        if (Objects.equals(eastBlock, itemId)) return true;
        if (Objects.equals(northBlock, itemId)) return true;
        if (Objects.equals(southBlock, itemId)) return true;
        return false;
    }
}
