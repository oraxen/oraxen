package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import io.th0rgal.oraxen.recipes.builders.SmithingBuilder;
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
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.util.Arrays;

public class SmithingRecipeEvents implements Listener {

    @EventHandler
    public void onSmithingRecipe(PrepareSmithingEvent event) {
        if (RecipesBuilderEvents.isBuilderInventory(event, SmithingBuilder.class)) {
            event.setResult(null);
            return;
        }

        SmithingInventory inventory = event.getInventory();
        ItemStack template = inventory.getInputTemplate();
        ItemStack material = inventory.getInputMineral();
        ItemStack input = inventory.getInputEquipment();

        if (ItemUtils.isEmpty(template) || ItemUtils.isEmpty(material) || ItemUtils.isEmpty(input)) return;
        Recipe selected = inventory.getRecipe();
        if (selected instanceof SmithingTransformRecipe smithing)
            selected = RecipesEventsManager.get().resolveSmithing(smithing, template, input, material);
        if (selected instanceof Keyed keyed
                && RecipesEventsManager.get().isConfiguredSmithingRecipe(keyed.getKey())) {
            // InventoryView is an abstract class below 1.21 but an interface on 1.21+; calling
            // event.getView().getPlayer() directly compiles to invokeinterface and throws
            // IncompatibleClassChangeError on 1.20.x. InventoryUtils handles both shapes.
            Player player = InventoryUtils.playerFromView(event);
            if (player == null || !RecipesEventsManager.get().hasSmithingPermission(player, keyed.getKey()))
                event.setResult(null);
            else if (selected instanceof SmithingTransformRecipe smithing) {
                if (!RecipesEventsManager.get().matchesSmithing(keyed.getKey(), template, input, material)
                        || !smithing.getTemplate().test(template) || !smithing.getBase().test(input)
                        || !smithing.getAddition().test(material)) event.setResult(null);
                else {
                    try {
                        event.setResult(RecipesEventsManager.get().smithingResult(keyed.getKey(), template, input, material, smithing.getResult()));
                    } catch (IllegalArgumentException | IllegalStateException exception) {
                        event.setResult(null);
                    }
                }
            }
            return;
        }
        if (Arrays.stream(inventory.getContents()).anyMatch(ItemUtils::isEmpty)) return;
        if (Arrays.stream(inventory.getContents()).noneMatch(OraxenItems::exists)) return;

        String oraxenItemId = OraxenItems.getIdByItem(input);
        if (oraxenItemId == null) return;
        MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(input);
        if (mechanic != null && mechanic.isAllowedInVanillaRecipes()) return;

        preventMismatchedVanillaSmithingRecipes(event, template, material, oraxenItemId);
    }

    private void preventMismatchedVanillaSmithingRecipes(PrepareSmithingEvent event, ItemStack template,
                                                         ItemStack material, String oraxenItemId) {
        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (!(recipe instanceof SmithingTransformRecipe smithing)) return;
            if (!smithing.getTemplate().test(template) || !smithing.getAddition().test(material)) return;
            if (oraxenItemId.equals(OraxenItems.getIdByItem(smithing.getBase().getItemStack()))) return;
            event.setResult(null);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmithItem(SmithItemEvent event) {
        RecipesEventsManager recipes = RecipesEventsManager.get();
        SmithingInventory inventory = event.getInventory();
        Recipe selected = inventory.getRecipe();
        if (selected instanceof SmithingTransformRecipe smithing)
            selected = recipes.resolveSmithing(smithing, inventory.getInputTemplate(),
                    inventory.getInputEquipment(), inventory.getInputMineral());
        if (!(selected instanceof Keyed keyed)) return;
        if (recipes.isConfiguredSmithingRecipe(keyed.getKey())
                && (!recipes.hasSmithingPermission(event.getWhoClicked(), keyed.getKey())
                || recipes.hasSmithingTransform(keyed.getKey()) && event.isShiftClick()
                || !recipes.matchesSmithing(keyed.getKey(), event.getInventory().getInputTemplate(),
                event.getInventory().getInputEquipment(), event.getInventory().getInputMineral())
                || selected instanceof SmithingTransformRecipe smithing
                && (!smithing.getTemplate().test(event.getInventory().getInputTemplate())
                || !smithing.getBase().test(event.getInventory().getInputEquipment())
                || !smithing.getAddition().test(event.getInventory().getInputMineral()))))
            event.setCancelled(true);
    }
}
