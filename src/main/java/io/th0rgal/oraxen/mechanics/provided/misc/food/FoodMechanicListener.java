package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Objects;

public class FoodMechanicListener implements Listener {
    private FoodMechanicFactory factory;

    public FoodMechanicListener(FoodMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler
    public void onEatingFood(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String itemID = OraxenItems.getIdByItem(event.getItem());
        if (itemID == null || factory.isNotImplementedIn(itemID))
            return;
        if (player.getGameMode() == GameMode.CREATIVE)
            return;
        FoodMechanic mechanic = (FoodMechanic) factory.getMechanic(itemID);
        event.setCancelled(true);
        if (!mechanic.hasReplacement())
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        else if (mechanic.hasReplacement()) {
            if (mechanic.isVanillaItem() && mechanic.getReplacementItem() != null)
                player.getInventory().setItemInMainHand(new ItemStack(Objects.requireNonNull(Material.getMaterial(mechanic.getReplacementItem()))));
            else if (!mechanic.isVanillaItem() && mechanic.getReplacementItem() != null)
                player.getInventory().setItemInMainHand(OraxenItems.getItemById(mechanic.getReplacementItem()).build());
        }
        if (mechanic.isHasEffect() && !mechanic.getEffects().isEmpty()) {
            for (PotionEffect effect : mechanic.getEffects()) {
                player.addPotionEffect(effect);
            }
        }
        int foodLevel = mechanic.getHunger();
        int saturation = mechanic.getSaturation();
        player.setFoodLevel(player.getFoodLevel() + Math.min(foodLevel, 20));
        player.setSaturation(player.getSaturation() + Math.min(saturation, 20));
    }
}
