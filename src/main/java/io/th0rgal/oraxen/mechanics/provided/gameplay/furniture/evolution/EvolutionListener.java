package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Random;

public class EvolutionListener implements Listener {
    private final MechanicFactory factory;
    public EvolutionListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBoneMeal(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        System.out.println("item interacted: " + item.toString());
        RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(player.getLocation().add(0,player.getEyeHeight(),0), player.getEyeLocation().getDirection(), 5.0, e -> e.getType() == EntityType.ITEM_FRAME);
        if (rayTraceResult == null)
            return;
        ItemFrame crop = (ItemFrame) rayTraceResult.getHitEntity();
        if (rayTraceResult == null)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND)
            return;
        if (item == null || item.getType() != Material.BONE_MEAL)
            return;
        if (!crop.getPersistentDataContainer().has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER))
            return;
        ItemStack itemInFrame = crop.getItem();
        String cropItemID = OraxenItems.getIdByItem(itemInFrame);
        FurnitureMechanic mechanic = (FurnitureMechanic) factory.getMechanic(cropItemID);
        if (!mechanic.getEvolution().getAllowBoneMeal())
            return;
        if (mechanic.getEvolution().getNextStage() == null)
            return;
        item.setAmount(item.getAmount() - 1);
        crop.getLocation().getWorld().playEffect(crop.getLocation(), Effect.BONE_MEAL_USE, 3);
        if (isRandom(Integer.valueOf(mechanic.getEvolution().getBoneMealChance()))) {
            mechanic.remove(crop);
            FurnitureMechanic nextMechanic = (FurnitureMechanic) factory.getMechanic(mechanic.getEvolution().getNextStage());
            nextMechanic.place(crop.getRotation(), mechanic.getYaw(crop.getRotation()), crop.getFacing(), crop.getLocation(), null);
        }
    }

    public static Boolean isRandom(int size) {
        switch (size) {
            case 0: return false;
            case 1:{
                return ((new Random()).nextFloat() + "").endsWith("1");
            }
            case 2:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2"));
            }
            case 3:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3"));
            }
            case 4:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4"));
            }
            case 5:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5"));
            }
            case 6:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6"));
            }
            case 7:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6") || ((new Random()).nextFloat() + "").endsWith("7"));
            }
            case 8:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6") || ((new Random()).nextFloat() + "").endsWith("7") || ((new Random()).nextFloat() + "").endsWith("8"));
            }
            case 9:{
                return (((new Random()).nextFloat() + "").endsWith("1") || ((new Random()).nextFloat() + "").endsWith("2") || ((new Random()).nextFloat() + "").endsWith("3") || ((new Random()).nextFloat() + "").endsWith("4") || ((new Random()).nextFloat() + "").endsWith("5") || ((new Random()).nextFloat() + "").endsWith("6") || ((new Random()).nextFloat() + "").endsWith("7") || ((new Random()).nextFloat() + "").endsWith("8") || ((new Random()).nextFloat() + "").endsWith("9"));
            }
            default: return true;
        }
    }
}
