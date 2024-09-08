package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.customblockdata.CustomBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class RegularNoteBlock {

    private final Block block;
    private final Block blockAbove;
    private final NamespacedKey noteKey;
    private final NamespacedKey poweredKey;
    private final @Nullable Player player;
    private final PersistentDataContainer container;
    private final boolean isPowered;
    private final byte note;
    private final float pitch;
    private final String sound;

    public RegularNoteBlock(Block block, @Nullable Player player) {
        this.container = new CustomBlockData(block, OraxenPlugin.get());
        this.block = block;
        this.blockAbove = block.getRelative(BlockFace.UP);
        this.noteKey = new NamespacedKey(OraxenPlugin.get(), "note");
        this.poweredKey = new NamespacedKey(OraxenPlugin.get(), "powered");
        this.player = player;
        this.isPowered = container.getOrDefault(poweredKey, PersistentDataType.BOOLEAN, false);
        this.note = container.getOrDefault(noteKey, PersistentDataType.BYTE, (byte) 0);
        this.pitch = (float) Math.pow(2.0F, (note - 12F) / 12F);
        this.sound = switch (blockAbove.getType()) {
            case SKELETON_SKULL -> "block.note_block.imitate.skeleton";
            case PIGLIN_HEAD -> "block.note_block.imitate.piglin";
            case ZOMBIE_HEAD -> "block.note_block.imitate.zombie";
            case CREEPER_HEAD -> "block.note_block.imitate.creeper";
            case DRAGON_HEAD -> "block.note_block.imitate.ender_dragon";
            case WITHER_SKELETON_SKULL -> "block.note_block.imitate.wither_skeleton";
            case PLAYER_HEAD -> {
                NamespacedKey playerHeadSound = ((Skull) blockAbove.getState()).getNoteBlockSound();
                yield playerHeadSound != null ? playerHeadSound.value() : null;
            }

            default -> {
                Block blockBelow = block.getRelative(BlockFace.DOWN);
                NoteBlockMechanic noteBlockMechanic = OraxenBlocks.getNoteBlockMechanic(blockBelow);
                yield noteBlockMechanic != null ? noteBlockMechanic.getInstrument().toLowerCase() :
                        "block.note_block." + NMSHandlers.getHandler().getNoteBlockInstrument(blockBelow).toLowerCase();
            }
        };
    }

    public String getSound() {
        return sound;
    }

    public Byte getNote() {
        return note;
    }

    public Float getPitch() {
        return pitch;
    }

    public void runClickAction(Action action) {
        playSoundNaturally();
        if (action == Action.RIGHT_CLICK_BLOCK) increaseNote();
    }

    public void playSoundNaturally() {
        if (!blockAbove.isEmpty() && !isMobSound()) return;

        Location loc = BlockHelpers.toCenterBlockLocation(block.getLocation());
        World world = block.getWorld();
        double particleColor = (double) note / 24.0;

        if (!isMobSound()) {
            world.playSound(loc, sound, 1.0F, pitch);
            world.spawnParticle(Particle.NOTE, loc.add(0, 1.2, 0), 0, particleColor, 0, 0, 1);
        } else world.playSound(loc, sound, 1.0F, 1.0F);

        world.sendGameEvent(player, GameEvent.NOTE_BLOCK_PLAY, loc.toVector());
    }

    public void increaseNote() {
        container.set(noteKey, PersistentDataType.BYTE, (byte) ((note + 1) % 25));
    }

    public void setPowered(boolean powered) {
        if (powered) container.set(poweredKey, PersistentDataType.BOOLEAN, true);
        else container.remove(poweredKey);
    }

    public void removeData() {
        container.remove(noteKey);
        container.remove(poweredKey);
    }

    public boolean isMobSound() {
        return switch (blockAbove.getType()) {
            case SKELETON_SKULL, ZOMBIE_HEAD, PIGLIN_HEAD, CREEPER_HEAD, DRAGON_HEAD, WITHER_SKELETON_SKULL, PLAYER_HEAD -> true;
            default -> false;
        };
    }

    public boolean isPowered() {
        return isPowered;
    }
}
