package io.th0rgal.oraxen.utils.tuneblock;

import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;

public class TuneBlockListeners implements Listener {

    @EventHandler
    public void onNoteBlockPlay(final NotePlayEvent event) {
        Block block = event.getBlock();
        if (!BlockHelpers.isRegularNoteBlock(block)) return;

        TuneBlock tuneBlock = new TuneBlock(block, null);
        tuneBlock.runClickAction(Action.LEFT_CLICK_BLOCK);
        event.setCancelled(true);
    }

    @EventHandler
    public void onNoteBlockPower(final BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (!BlockHelpers.isRegularNoteBlock(block)) return;
        TuneBlock tuneBlock = new TuneBlock(block, null);

        if (!block.isBlockIndirectlyPowered()) {
            tuneBlock.setPowered(false);
            return;
        }

        if (!tuneBlock.isPowered()) tuneBlock.playSoundNaturally();
        tuneBlock.setPowered(true);
    }

    @EventHandler
    public void onRightClickNoteBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !BlockHelpers.isRegularNoteBlock(block)) return;

        Player player = event.getPlayer();
        PlayerInventory playerInventory = player.getInventory();
        ItemStack mainHandItem = playerInventory.getItemInMainHand();

        if (mainHandItem.getItemMeta() instanceof SkullMeta && event.getBlockFace() == BlockFace.UP) return;

        ItemStack offHandItem = playerInventory.getItemInOffHand();

        boolean isSneaking = player.isSneaking();
        boolean isMainHandEmpty = mainHandItem.isEmpty();
        boolean isOffHandEmpty = offHandItem.isEmpty();

        TuneBlock tuneBlock = new TuneBlock(block, player);

        if (!(tuneBlock.isMobSound() && !(isSneaking && (!isMainHandEmpty || !isOffHandEmpty)) || !(isSneaking && (!isMainHandEmpty || !isOffHandEmpty))))
            return;

        event.setUseInteractedBlock(Event.Result.DENY);
        tuneBlock.runClickAction(Action.RIGHT_CLICK_BLOCK);
    }
}
