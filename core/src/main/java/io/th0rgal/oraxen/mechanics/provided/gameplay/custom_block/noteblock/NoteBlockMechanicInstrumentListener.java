package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock;

import io.th0rgal.oraxen.api.OraxenBlocks;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;

public class NoteBlockMechanicInstrumentListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlay(final NotePlayEvent event) {
        Block block = event.getBlock();
        event.setCancelled(true);
        if (OraxenBlocks.isOraxenNoteBlock(block)) return;

        RegularNoteBlock regularNoteBlock = new RegularNoteBlock(block, null);
        regularNoteBlock.runClickAction(Action.LEFT_CLICK_BLOCK);
        event.setCancelled(true);
    }

    @EventHandler
    public void onNoteBlockPower(final BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NOTE_BLOCK || OraxenBlocks.isOraxenNoteBlock(block)) return;

        RegularNoteBlock regularNoteBlock = new RegularNoteBlock(block, null);
        if (!block.isBlockIndirectlyPowered()) {
            regularNoteBlock.setPowered(false);
            return;
        }

        if (!regularNoteBlock.isPowered()) regularNoteBlock.playSoundNaturally();
        regularNoteBlock.setPowered(true);
    }

    @EventHandler
    public void onRightClickNoteBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.NOTE_BLOCK || OraxenBlocks.isOraxenNoteBlock(block)) return;

        Player player = event.getPlayer();
        PlayerInventory playerInventory = player.getInventory();
        ItemStack mainHandItem = playerInventory.getItemInMainHand();

        if (mainHandItem.getItemMeta() instanceof SkullMeta && event.getBlockFace() == BlockFace.UP) return;

        ItemStack offHandItem = playerInventory.getItemInOffHand();

        RegularNoteBlock regularNoteBlock = new RegularNoteBlock(block, player);
        boolean isSneakingWithHandOccupied = player.isSneaking() && (!mainHandItem.isEmpty() || !offHandItem.isEmpty());
        if (isSneakingWithHandOccupied) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        regularNoteBlock.runClickAction(Action.RIGHT_CLICK_BLOCK);
    }
}
