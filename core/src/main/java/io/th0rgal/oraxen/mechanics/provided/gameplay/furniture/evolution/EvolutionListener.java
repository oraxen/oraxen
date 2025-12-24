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

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.EVOLUTION_KEY;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.STAGE_INDEX_KEY;

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

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(EVOLUTION_KEY, PersistentDataType.INTEGER)) return;

        ItemStack itemInteracted = player.getInventory().getItemInMainHand();
        if (itemInteracted.getType() != Material.BONE_MEAL) return;

        event.setCancelled(true);

        // NEW: Handle staged evolution
        if (mechanic.hasGrowthStages()) {
            handleStagedBoneMeal(entity, mechanic, pdc, itemInteracted);
            return;
        }

        // Legacy: Handle single-stage evolution
        handleLegacyBoneMeal(entity, mechanic, pdc, itemInteracted);
    }

    /**
     * Handles bone meal for furniture using the new inline stages system.
     */
    private void handleStagedBoneMeal(Entity entity, FurnitureMechanic mechanic, 
                                       PersistentDataContainer pdc, ItemStack boneMeal) {
        int currentStageIndex = pdc.getOrDefault(STAGE_INDEX_KEY, PersistentDataType.INTEGER, 0);
        
        // Check if at final stage
        if (mechanic.isFinalStage(currentStageIndex)) return;
        
        GrowthStage currentStage = mechanic.getGrowthStage(currentStageIndex);
        if (currentStage == null || !currentStage.isBoneMeal()) return;
        
        // Consume bone meal and play effect
        boneMeal.setAmount(boneMeal.getAmount() - 1);
        entity.getWorld().playEffect(entity.getLocation(), Effect.BONE_MEAL_USE, 3);
        
        // Check bone meal success chance
        if (!randomChance(currentStage.getBoneMealChance())) return;
        
        // Advance to next stage
        int nextStageIndex = currentStageIndex + 1;
        GrowthStage nextStage = mechanic.getGrowthStage(nextStageIndex);
        if (nextStage == null) return;
        
        // Update stage index and reset evolution timer
        pdc.set(STAGE_INDEX_KEY, PersistentDataType.INTEGER, nextStageIndex);
        pdc.set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        
        // Swap the model (no entity recreation!)
        FurnitureMechanic.setFurnitureItemModel(entity, mechanic.getItemID(), nextStage.getModelKey());
    }

    /**
     * Handles bone meal for furniture using the legacy evolution system.
     */
    private void handleLegacyBoneMeal(Entity entity, FurnitureMechanic mechanic, 
                                       PersistentDataContainer pdc, ItemStack boneMeal) {
        EvolvingFurniture evolution = mechanic.getEvolution();
        if (evolution == null || !evolution.isBoneMeal() || evolution.getNextStage() == null) return;
        
        FurnitureMechanic nextMechanic = (FurnitureMechanic) FurnitureFactory.instance.getMechanic(evolution.getNextStage());
        if (nextMechanic == null) return;

        boneMeal.setAmount(boneMeal.getAmount() - 1);
        entity.getWorld().playEffect(entity.getLocation(), Effect.BONE_MEAL_USE, 3);
        
        if (randomChance(evolution.getBoneMealChance())) {
            OraxenFurniture.remove(entity, null);
            nextMechanic.place(entity.getLocation(), entity.getLocation().getYaw(), entity.getFacing());
        }
    }

    private boolean randomChance(double chance) {
        return Math.random() * 100 <= chance;
    }
}
