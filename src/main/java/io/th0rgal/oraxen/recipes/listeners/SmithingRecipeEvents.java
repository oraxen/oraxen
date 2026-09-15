package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import io.th0rgal.oraxen.utils.InventoryUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.util.Arrays;

public class SmithingRecipeEvents implements Listener {

    @EventHandler
    public void onSmithingRecipe(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack template = inventory.getInputTemplate();
        ItemStack material = inventory.getInputMineral();
        ItemStack input = inventory.getInputEquipment();

        if (ItemUtils.isEmpty(template) || ItemUtils.isEmpty(material) || ItemUtils.isEmpty(input)) return;
        if (inventory.getRecipe() instanceof Keyed keyed
                && RecipesEventsManager.get().isConfiguredSmithingRecipe(keyed.getKey())) {
            // InventoryView is an abstract class below 1.21 but an interface on 1.21+; calling
            // event.getView().getPlayer() directly compiles to invokeinterface and throws
            // IncompatibleClassChangeError on 1.20.x. InventoryUtils handles both shapes.
            Player player = InventoryUtils.playerFromView(event);
            if (player == null || !RecipesEventsManager.get().hasSmithingPermission(player, keyed.getKey()))
                event.setResult(null);
            return;
        }
        if (Arrays.stream(inventory.getContents()).anyMatch(ItemUtils::isEmpty)) return;
        if (Arrays.stream(inventory.getContents()).noneMatch(OraxenItems::exists)) return;

        String oraxenItemId = OraxenItems.getIdByItem(input);
        if (oraxenItemId == null) return;
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(input);
        if (mechanic != null && mechanic.isAllowedInVanillaRecipes()) return;

        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (!(recipe instanceof SmithingTransformRecipe smithing)) return;
            if (!smithing.getTemplate().test(template) || !smithing.getAddition().test(material)) return;
            if (oraxenItemId.equals(OraxenItems.getIdByItem(smithing.getBase().getItemStack()))) return;
            event.setResult(null);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        if (!(event.getInventory().getRecipe() instanceof Keyed keyed)) return;
        RecipesEventsManager recipes = RecipesEventsManager.get();
        if (recipes.isConfiguredSmithingRecipe(keyed.getKey())
                && !recipes.hasSmithingPermission(event.getWhoClicked(), keyed.getKey()))
            event.setCancelled(true);
    }
}
