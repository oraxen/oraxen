package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class WateringMechanicListener implements Listener {

    private final MechanicFactory factory;

    public WateringMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onRefillCan(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = OraxenItems.getIdByItem(item);
        WateringMechanic mechanic = (WateringMechanic) factory.getMechanic(itemId);
        Block block = event.getClickedBlock();
        if (item.getType() == Material.AIR || factory.isNotImplementedIn(itemId) || !mechanic.isEmpty()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (player.getTargetBlockExact(5, FluidCollisionMode.SOURCE_ONLY).getType() == Material.WATER) {
            player.getInventory().setItemInMainHand(OraxenItems.getItemById(mechanic.getFilledCanItem()).build());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
            Bukkit.broadcastMessage("test");
        }

        if (block.getType() == Material.WATER_CAULDRON) {
            Levelled cauldron = (Levelled) block.getBlockData();
            if (cauldron.getLevel() == 1) return;
            cauldron.setLevel(cauldron.getLevel()-1);
            block.setBlockData(cauldron);
            player.getInventory().setItemInMainHand(OraxenItems.getItemById(mechanic.getFilledCanItem()).build());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWateringFarmBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = OraxenItems.getIdByItem(item);
        WateringMechanic mechanic = (WateringMechanic) factory.getMechanic(itemId);

        if (item.getType() == Material.AIR || factory.isNotImplementedIn(itemId) || !mechanic.isFilled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();

        if (block.getType() == Material.NOTE_BLOCK && getNoteBlockMechanic(block).hasDryout()) {
            FarmBlockDryout farmMechanic = getNoteBlockMechanic(block).getDryout();
            PersistentDataContainer farmBlockData = new CustomBlockData(block, OraxenPlugin.get());

            NoteBlockMechanicFactory.setBlockModel(block, farmMechanic.getMoistFarmBlock());
            farmBlockData.set(FARMBLOCK_KEY, PersistentDataType.INTEGER, farmMechanic.getDryoutTime());
        } else if (block.getType() == Material.FARMLAND) {
            Farmland data = ((Farmland) block.getBlockData());
            if (data.getMoisture() == data.getMaximumMoisture()) return;
            data.setMoisture(data.getMaximumMoisture());
            block.setBlockData(data);
        } else return;

        player.getInventory().setItemInMainHand(OraxenItems.getItemById(mechanic.getEmptyCanItem()).build());
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, block.getLocation().add(0.5, 1, 0.5), 40);
        player.getWorld().playSound(block.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
    }
}
