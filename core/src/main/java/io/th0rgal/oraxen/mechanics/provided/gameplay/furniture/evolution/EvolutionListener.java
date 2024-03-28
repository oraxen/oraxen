package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EvolutionListener implements Listener {

    public EvolutionListener() {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBoneMeal(OraxenFurnitureInteractEvent event) {
        if (event.hand() != EquipmentSlot.HAND) return;
        Entity baseEntity = event.baseEntity();
        FurnitureMechanic mechanic = event.mechanic();

        if (!mechanic.hasEvolution()) return;
        PersistentDataContainer cropPDC = baseEntity.getPersistentDataContainer();
        if (!cropPDC.has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER)) return;

        ItemStack itemInteracted = event.itemInHand();
        if (itemInteracted.getType() != Material.BONE_MEAL) return;

        event.setCancelled(true);
        EvolvingFurniture evolution = mechanic.getEvolution();
        if (!evolution.isBoneMeal() || evolution.getNextStage() == null) return;
        FurnitureMechanic nextMechanic = (FurnitureMechanic) FurnitureFactory.instance.getMechanic(evolution.getNextStage());
        if (nextMechanic == null) return;

        itemInteracted.setAmount(itemInteracted.getAmount() - 1);
        baseEntity.getWorld().playEffect(baseEntity.getLocation(), Effect.BONE_MEAL_USE, 3);
        if (randomChance(evolution.getBoneMealChance())) {

            OraxenFurniture.remove(baseEntity, null);
            nextMechanic.place(baseEntity.getLocation(), baseEntity.getLocation().getYaw(), baseEntity.getFacing());
        }
    }

    public boolean randomChance(double chance) {
        return Math.random() <= chance;
    }
}
