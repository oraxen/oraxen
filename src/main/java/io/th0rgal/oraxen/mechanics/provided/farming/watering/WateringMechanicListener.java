package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

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

        if (item.getType() == Material.AIR || factory.isNotImplementedIn(itemId) || !mechanic.isWateringCan()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        NoteBlockMechanic farmMechanic = getNoteBlockMechanic(block);
        PersistentDataContainer farmBlockData = new CustomBlockData(block, OraxenPlugin.get());


        if (!farmMechanic.isFarmBlock() || Objects.equals(farmBlockData.get(FARMBLOCK_KEY, PersistentDataType.STRING), farmMechanic.getMoistFarmBlock())) return;
        NoteBlockMechanicFactory.setBlockModel(block, farmMechanic.getMoistFarmBlock());
        farmBlockData.set(FARMBLOCK_KEY, PersistentDataType.STRING, farmMechanic.getMoistFarmBlock());

        player.getWorld().spawnParticle(Particle.WATER_SPLASH, block.getLocation().add(0.5, 1, 0.5), 40);
    }

}
