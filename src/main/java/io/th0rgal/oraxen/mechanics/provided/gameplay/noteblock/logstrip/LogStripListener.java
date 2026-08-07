package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class LogStripListener implements Listener {

    @EventHandler
    public void onStrippingLog(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null) return;
        if (block.getType() != Material.NOTE_BLOCK || !canStripLog(item)) return;

        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;

        if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock() && !mechanic.isLog())
            mechanic = mechanic.getDirectional().getParentMechanic();

        LogStripping log = mechanic.getLog();
        if (!mechanic.isLog() || !log.canBeStripped()) return;

        if (log.hasStrippedDrop()) {
            player.getWorld().dropItemNaturally(
                    block.getRelative(player.getFacing().getOppositeFace()).getLocation(),
                    OraxenItems.getItemById(log.getStrippedLogDrop()).build());
        }

        if (log.shouldDecreaseAxeDurability() && player.getGameMode() != GameMode.CREATIVE) {
            if (item.getItemMeta() instanceof Damageable axeDurabilityMeta) {
                int durability = axeDurabilityMeta.getDamage();
                int maxDurability = item.getType().getMaxDurability();

                if (durability + 1 <= maxDurability) {
                    axeDurabilityMeta.setDamage(durability + 1);
                    item.setItemMeta(axeDurabilityMeta);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                    item.setType(Material.AIR);
                }
            }
        }

        NoteBlockMechanicFactory.setBlockModel(block, log.getStrippedLogBlock());
        player.playSound(block.getLocation(), Sound.ITEM_AXE_STRIP, 1.0f, 0.8f);
    }

    private boolean canStripLog(ItemStack itemStack) {
        if (itemStack.getType().toString().endsWith("_AXE")) return true;
        else if (MiscMechanicFactory.get() != null) {
            MiscMechanic miscMechanic = MiscMechanicFactory.get().getMechanic(OraxenItems.getIdByItem(itemStack));
            return miscMechanic != null && miscMechanic.canStripLogs();
        } else return false;
    }
}
