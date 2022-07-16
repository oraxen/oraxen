package io.th0rgal.oraxen.mechanics.provided.misc.replaceblock;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class ReplaceBlockMechanicListener implements Listener {
    private final ReplaceBlockMechanicFactory factory;

    public ReplaceBlockMechanicListener(ReplaceBlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        ReplaceBlockMechanic mechanic = (ReplaceBlockMechanic) factory.getMechanic(OraxenItems.getIdByItem(item));
        if (event.getAction() != Action.valueOf(mechanic.getAction()) || event.getHand() != EquipmentSlot.HAND)
            return;
        NoteBlockMechanic blockMechanic = getNoteBlockMechanic(block);
        if (blockMechanic == null)
            return;
        if (blockMechanic.getItemID() != mechanic.getBaseBlock())
            return;
        NoteBlockMechanicFactory.setBlockModel(block, mechanic.getSwitchedBlock());
        if (mechanic.getEffect() != null && mechanic.getEffectNumber() > 0) {
            block.getLocation().getWorld().playEffect(block.getLocation(), Effect.valueOf(mechanic.getEffect()), mechanic.getEffectNumber());
        }
        if (mechanic.getSound() != null) {
            player.playSound(player.getLocation(), Sound.valueOf(mechanic.getSound()), 500.0f, 1.0f);
        }
    }
}
