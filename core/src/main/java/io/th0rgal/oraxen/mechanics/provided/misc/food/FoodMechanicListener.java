package io.th0rgal.oraxen.mechanics.provided.misc.food;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class FoodMechanicListener implements Listener {
    private final FoodMechanicFactory factory;

    public FoodMechanicListener(FoodMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void registerReplacements(OraxenItemsLoadedEvent event) {
        for (String itemID : factory.getItems()) {
            FoodMechanic foodMechanic = (FoodMechanic) factory.getMechanic(itemID);
            ConfigurationSection replacementSection = foodMechanic.getSection().getConfigurationSection("replacement");
            if (replacementSection != null) foodMechanic.registerReplacement(replacementSection);
        }
    }

    @EventHandler
    public void onEatingFood(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack itemStack = event.getItem();
        String itemID = OraxenItems.getIdByItem(itemStack);
        if (itemID == null || factory.isNotImplementedIn(itemID)) return;
        FoodMechanic mechanic = (FoodMechanic) factory.getMechanic(itemID);

        // Still let replacement work on 1.20.5+ servers if it has food component due to replacement
        if (!VersionUtil.atOrAbove("1.20.5")) {
            event.setCancelled(true);

            if (player.getGameMode() != GameMode.CREATIVE) {
                ItemStack itemInHand = (event.getHand() == EquipmentSlot.HAND ? inventory.getItemInMainHand() : inventory.getItemInOffHand());
                ItemUtils.subtract(itemInHand, 1);


                if (mechanic.hasEffects() && Math.random() <= mechanic.getEffectProbability())
                    player.addPotionEffects(mechanic.getEffects());
            }

            player.setFoodLevel(player.getFoodLevel() + Math.min(mechanic.getHunger(), 20));
            player.setSaturation(player.getSaturation() + Math.min(mechanic.getSaturation(), 20));
        }/* else {
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasFood() && player.getGameMode() != GameMode.CREATIVE && mechanic.hasReplacement())
                inventory.addItem(mechanic.getReplacement());
        }*/


    }
}
