package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WateringMechanicListener implements Listener {

    private final MechanicFactory factory;

    public WateringMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWateringFarmBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = OraxenItems.getIdByItem(item);
        WateringMechanic mechanic = (WateringMechanic) factory.getMechanic(itemId);

        if (factory.isNotImplementedIn(itemId) || !mechanic.isWateringCan()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        NoteBlockMechanic farmingBlockMechanic = getNoteBlockMechanic(block);

        if (!farmingBlockMechanic.isFarmBlock()) return;
        NoteBlockMechanicFactory.setBlockModel(block, farmingBlockMechanic.getMoistFarmBlock());
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, block.getLocation(), 10);

    }

    private NoteBlockMechanic getNoteBlockMechanic(Block block) {
        final NoteBlock noteBlok = (NoteBlock) block.getBlockData();
        return NoteBlockMechanicFactory
                .getBlockMechanic((int) (noteBlok.getInstrument().getType()) * 25
                        + (int) noteBlok.getNote().getId() + (noteBlok.isPowered() ? 400 : 0) - 26);
    }

}
