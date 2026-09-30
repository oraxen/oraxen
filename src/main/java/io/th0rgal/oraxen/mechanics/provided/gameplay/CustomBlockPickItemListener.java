package io.th0rgal.oraxen.mechanics.provided.gameplay;

import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.inventories.PickItemUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class CustomBlockPickItemListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPickBlock(PlayerPickBlockEvent event) {
        ItemBuilder itemBuilder = getPickedItem(event.getBlock());
        if (itemBuilder == null) return;

        ItemStack item = itemBuilder.build();
        if (item == null || item.getType().isAir()) return;

        event.setCancelled(true);
        PickItemUtils.pickItem(event.getPlayer(), item);
    }

    private ItemBuilder getPickedItem(Block block) {
        String itemId = switch (block.getType()) {
            case NOTE_BLOCK -> getNoteBlockItemId(block);
            case TRIPWIRE -> getStringBlockItemId(block);
            case CHORUS_PLANT -> getChorusBlockItemId(block);
            default -> getShapedBlockItemId(block);
        };
        return itemId == null ? null : OraxenItems.getItemById(itemId);
    }

    private String getNoteBlockItemId(Block block) {
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return null;
        if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
            return mechanic.getDirectional().getParentBlock();
        return mechanic.getItemID();
    }

    private String getStringBlockItemId(Block block) {
        StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
        if (mechanic != null) return mechanic.getItemID();

        StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(block.getRelative(BlockFace.DOWN));
        return mechanicBelow != null && mechanicBelow.isTall() ? mechanicBelow.getItemID() : null;
    }

    private String getChorusBlockItemId(Block block) {
        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
        return mechanic == null ? null : mechanic.getItemID();
    }

    private String getShapedBlockItemId(Block block) {
        ShapedBlockMechanic mechanic = OraxenBlocks.getShapedMechanic(block);
        return mechanic == null ? null : mechanic.getItemID();
    }
}
