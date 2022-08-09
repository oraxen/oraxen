package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener.getNoteBlockMechanic;

public class EvolutionListener implements Listener {
    private final MechanicFactory factory;

    public EvolutionListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onBoneMeal(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        if (entity instanceof ItemFrame) {
            ItemFrame crop = (ItemFrame) entity;
            if (!crop.getPersistentDataContainer().has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER))
                return;
            ItemStack itemInteracted = player.getInventory().getItemInMainHand();
            if (itemInteracted.getType() != Material.BONE_MEAL)
                return;
            String itemID = crop.getPersistentDataContainer().get(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING);
            FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(itemID);
            Block blockBelow = crop.getLocation().clone().subtract(0, 1, 0).getBlock();
            if (mechanic == null)
                return;
            if (mechanic.farmlandRequired && blockBelow.getType() != Material.FARMLAND) {
                mechanic.remove(crop);
                return;
            }
            if (mechanic.farmblockRequired) {
                if (blockBelow.getType() != Material.NOTE_BLOCK) {
                    mechanic.remove(crop);
                    return;
                }
                NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(blockBelow);
                if (noteBlockMechanic.hasDryout()) {
                    if (!noteBlockMechanic.getDryout().isFarmBlock()) {
                        mechanic.remove(crop);
                        return;
                    } else if (!noteBlockMechanic.getDryout().isMoistFarmBlock()) {
                        crop.getPersistentDataContainer().set(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
                        return;
                    }
                }
            }
            if (!mechanic.getEvolution().isBoneMeal())
                return;
            if (mechanic.getEvolution().getNextStage() == null)
                return;
            itemInteracted.setAmount(itemInteracted.getAmount() - 1);
            crop.getLocation().getWorld().playEffect(crop.getLocation(), Effect.BONE_MEAL_USE, 3);
            if (randomChance((double) mechanic.getEvolution().getBoneMealChance())) {
                mechanic.remove(crop);
                FurnitureMechanic nextMechanic = (FurnitureMechanic) factory.getMechanic(mechanic.getEvolution().getNextStage());
                nextMechanic.place(crop.getRotation(), mechanic.getYaw(crop.getRotation()), crop.getFacing(), crop.getLocation(), null);
            }
        }
    }
    public boolean randomChance(double chance) {
        return Math.random() <= chance;
    }
}
