package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class LogStripListener implements Listener {

    private final MechanicFactory factory;

    public LogStripListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onStrippingLog(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.NOTE_BLOCK) return;
        if (player.getInventory().getItemInMainHand().getType().toString().contains("_AXE")) {
            NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
            if (mechanic == null) return;
            if (mechanic.isDirectional() && !mechanic.isLog())
                mechanic = (NoteBlockMechanic) factory.getMechanic(mechanic.getDirectional().getParentBlock());

            if (mechanic.isLog() && mechanic.getLog().canBeStripped()) {
                if (mechanic.getLog().hasStrippedDrop()) {
                    player.getWorld().dropItemNaturally(
                            block.getRelative(player.getFacing().getOppositeFace()).getLocation(),
                            OraxenItems.getItemById(mechanic.getLog().getStrippedLogDrop()).build());
                }
                NoteBlockMechanicFactory.setBlockModel(block, mechanic.getLog().getStrippedLogBlock());
                player.playSound(block.getLocation(), Sound.ITEM_AXE_STRIP, 1.0f, 0.8f);
            }
        }
    }
}
