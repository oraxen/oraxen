package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public class EvolutionListener implements Listener {
    private final MechanicFactory factory;

    public EvolutionListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onBoneMeal(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        if (entity instanceof ItemFrame crop) {
            PersistentDataContainer cropPDC = crop.getPersistentDataContainer();
            if (!cropPDC.has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER)) return;

            ItemStack itemInteracted = player.getInventory().getItemInMainHand();
            if (itemInteracted.getType() != Material.BONE_MEAL) return;

            Block blockBelow = crop.getLocation().getBlock().getRelative(BlockFace.DOWN);
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(crop);
            if (mechanic == null) return;

            if (mechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                OraxenFurniture.remove(crop.getLocation(), event.getPlayer());
            } else if (mechanic.farmblockRequired) {
                NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(blockBelow);
                if (noteMechanic == null) {
                    OraxenFurniture.remove(crop.getLocation(), event.getPlayer());
                } else if (noteMechanic.hasDryout()) {
                    if (!noteMechanic.getDryout().isFarmBlock()) {
                        OraxenFurniture.remove(crop.getLocation(), event.getPlayer());
                        return;
                    } else if (!noteMechanic.getDryout().isMoistFarmBlock()) {
                        crop.getPersistentDataContainer().set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
                        return;
                    }
                }
            }
            
            if (!mechanic.getEvolution().isBoneMeal()) return;
            if (mechanic.getEvolution().getNextStage() == null) return;

            itemInteracted.setAmount(itemInteracted.getAmount() - 1);
            crop.getWorld().playEffect(crop.getLocation(), Effect.BONE_MEAL_USE, 3);
            if (randomChance(mechanic.getEvolution().getBoneMealChance())) {
                OraxenFurniture.remove(crop.getLocation(), event.getPlayer());
                OraxenFurniture.place(crop.getLocation(), mechanic.getEvolution().getNextStage(), crop.getRotation(), crop.getFacing());
            }
        }
    }
    public boolean randomChance(double chance) {
        return Math.random() <= chance;
    }
}
