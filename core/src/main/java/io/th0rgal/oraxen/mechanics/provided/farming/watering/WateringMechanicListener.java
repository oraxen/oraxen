package io.th0rgal.oraxen.mechanics.provided.farming.watering;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockDryout;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.wrappers.ParticleWrapper;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;

public class WateringMechanicListener implements Listener {

    private final MechanicFactory factory;

    public WateringMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onRefillCan(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = OraxenItems.getIdByItem(item);
        WateringMechanic mechanic = (WateringMechanic) factory.getMechanic(itemId);
        Block targetBlock = player.getTargetBlockExact(5, FluidCollisionMode.SOURCE_ONLY);

        if (item.getType() == Material.AIR || factory.isNotImplementedIn(itemId) || !mechanic.isEmpty()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null) return;

        if (targetBlock != null && targetBlock.getType() == Material.WATER) {
            player.getInventory().setItemInMainHand(OraxenItems.getItemById(mechanic.getFilledCanItem()).build());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
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
        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = OraxenItems.getIdByItem(item);
        WateringMechanic mechanic = (WateringMechanic) factory.getMechanic(itemId);

        if (item.getType() == Material.AIR || factory.isNotImplementedIn(itemId) || !mechanic.isFilled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null) return;
        NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (noteMechanic != null && noteMechanic.hasDryout() && !noteMechanic.getDryout().isMoistFarmBlock()) {
            FarmBlockDryout farmMechanic = noteMechanic.getDryout();
            NoteBlockMechanicFactory.setBlockModel(block, farmMechanic.getMoistFarmBlock());
            BlockHelpers.getPDC(block).set(FARMBLOCK_KEY, PersistentDataType.INTEGER, farmMechanic.getDryoutTime());
        } else if (block.getType() == Material.FARMLAND) {
            Farmland data = ((Farmland) block.getBlockData());
            if (data.getMoisture() == data.getMaximumMoisture()) return;
            data.setMoisture(data.getMaximumMoisture());
            block.setBlockData(data);
        } else return;

        player.getInventory().setItemInMainHand(OraxenItems.getItemById(mechanic.getEmptyCanItem()).build());
        player.getWorld().spawnParticle(ParticleWrapper.SPLASH, block.getLocation().add(0.5, 1, 0.5), 40);
        player.getWorld().playSound(block.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
    }
}
