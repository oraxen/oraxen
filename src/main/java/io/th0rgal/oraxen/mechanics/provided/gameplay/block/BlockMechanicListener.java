package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BlockMechanicListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConfiguredBlockDurabilityDamage(PlayerItemDamageEvent event) {
        BlockDurability.cancelPendingVanillaDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockEventClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Mechanic mechanic = OraxenBlocks.getOraxenBlock(block.getLocation());
        if (mechanic == null) return;

        Player player = event.getPlayer();
        if (!AntiGriefLib.canInteract(player, block.getLocation())) return;

        runBlockEvents(mechanic, player, action);
    }

    private void runBlockEvents(Mechanic mechanic, Player player, Action action) {
        if (mechanic instanceof NoteBlockMechanic noteBlockMechanic) {
            noteBlockMechanic.runBlockEvents(player, action);
        } else if (mechanic instanceof StringBlockMechanic stringBlockMechanic) {
            stringBlockMechanic.runBlockEvents(player, action);
        } else if (mechanic instanceof ChorusBlockMechanic chorusBlockMechanic) {
            chorusBlockMechanic.runBlockEvents(player, action);
        } else if (mechanic instanceof ShapedBlockMechanic shapedBlockMechanic) {
            shapedBlockMechanic.runBlockEvents(player, action);
        }
    }
}
