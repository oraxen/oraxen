package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.swap;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class SwapListener implements Listener {
    private final MechanicFactory factory;

    public SwapListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block.getType() != Material.NOTE_BLOCK)
            return;
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null)
            return;
        if (event.getAction() != Action.valueOf(mechanic.getSwapMechanic().getAction()) || event.getHand() != EquipmentSlot.HAND)
            return;
        if (mechanic.isDirectional()) {
            mechanic = (NoteBlockMechanic) factory.getMechanic(mechanic.getDirectional().getParentBlock());
        }
        NoteBlockMechanicFactory.setBlockModel(block, mechanic.getSwapMechanic().getSwitchedBlock());
    }
}
