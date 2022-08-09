package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

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
        FoodMechanic mechanic = (FoodMechanic) factory.getMechanic(itemID);
        event.setCancelled(true);
        if (player.getGameMode() != GameMode.CREATIVE)
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        int foodLevel = mechanic.getHunger();
        int saturation = mechanic.getSaturation();
        if (foodLevel > 20)
            foodLevel = 20;
        else if (foodLevel < 0)
            foodLevel = 0;
        if (saturation > 20)
            saturation = 20;
        else if (saturation < 0)
            saturation = 0;
        player.setFoodLevel(player.getFoodLevel() + foodLevel);
        player.setSaturation(player.getSaturation() + saturation);
    }
}
