package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EvolutionListener implements Listener {

    public EvolutionListener() {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBoneMeal(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null || !mechanic.hasEvolution()) return;
        // Swap entity to baseEntity to handle 1.19.4 Interaction entities
        entity = mechanic.getBaseEntity(entity);
        if (entity == null) return;

        PersistentDataContainer cropPDC = entity.getPersistentDataContainer();
        if (!cropPDC.has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER)) return;

        ItemStack itemInteracted = player.getInventory().getItemInMainHand();
        if (itemInteracted.getType() != Material.BONE_MEAL) return;

        event.setCancelled(true);
        EvolvingFurniture evolution = mechanic.getEvolution();
        if (!evolution.isBoneMeal() || evolution.getNextStage() == null) return;
        FurnitureMechanic nextMechanic = (FurnitureMechanic) FurnitureFactory.instance.getMechanic(evolution.getNextStage());
        if (nextMechanic == null) return;

        itemInteracted.setAmount(itemInteracted.getAmount() - 1);
        entity.getWorld().playEffect(entity.getLocation(), Effect.BONE_MEAL_USE, 3);
        if (randomChance(evolution.getBoneMealChance())) {

            OraxenFurniture.remove(entity, null);
            nextMechanic.place(entity.getLocation(), entity.getLocation().getYaw(), entity.getFacing());
        }
    }
    public boolean randomChance(double chance) {
        return Math.random() <= chance;
    }
}
