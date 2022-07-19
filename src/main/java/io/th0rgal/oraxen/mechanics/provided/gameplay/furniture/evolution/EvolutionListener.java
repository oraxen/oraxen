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

import java.util.Random;

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
            if (!mechanic.getEvolution().getAllowBoneMeal())
                return;
            if (mechanic.getEvolution().getNextStage() == null)
                return;
            itemInteracted.setAmount(itemInteracted.getAmount() - 1);
            crop.getLocation().getWorld().playEffect(crop.getLocation(), Effect.BONE_MEAL_USE, 3);
            if (isBoneMealChance(Integer.valueOf(mechanic.getEvolution().getBoneMealChance()))) {
                mechanic.remove(crop);
                FurnitureMechanic nextMechanic = (FurnitureMechanic) factory.getMechanic(mechanic.getEvolution().getNextStage());
                nextMechanic.place(crop.getRotation(), mechanic.getYaw(crop.getRotation()), crop.getFacing(), crop.getLocation(), null);
            }
        }
    }
    public int getRandWithinRange(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    public static Boolean isBoneMealChance(int chance) {
        switch (chance) {
            case 0:
                return false;
            case 1:
                return ((new Random()).nextFloat() + "").endsWith("1");
            case 2:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2"));
            case 3:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3"));
            case 4:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4"));
            case 5:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5"));
            case 6:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6"));
            case 7:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6") || ((new Random()).nextFloat() + "").endsWith("7"));
            case 8:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6") || ((new Random()).nextFloat() + "").endsWith("7") || ((new Random()).nextFloat() + "").endsWith("8"));
            case 9:
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6") || ((new Random()).nextFloat() + "").endsWith("7") || ((new Random()).nextFloat() + "").endsWith("8") || ((new Random()).nextFloat() + "").endsWith("9"));
            default:
                return true;
        }
    }
}
