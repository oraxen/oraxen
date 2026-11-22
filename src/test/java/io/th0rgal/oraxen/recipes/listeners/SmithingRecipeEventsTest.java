package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import io.th0rgal.oraxen.recipes.builders.SmithingBuilder;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmithingRecipeEventsTest {
    @Test
    void leavesPapersPreparedItemUntouchedWithoutAnExplicitTransform() {
        checkPreparedResult(false, true, false);
    }

    @Test
    void stillAppliesExplicitTransforms() {
        checkPreparedResult(true, true, false);
    }

    @Test
    void stillRejectsRecipesWithoutPermission() {
        checkPreparedResult(false, false, false);
    }

    @Test
    void replacesPapersResultWhenAnotherRecipeMatches() {
        checkPreparedResult(false, true, true);
    }

    @Test
    void blocksVanillaUpgradesOfCustomItemsWhenTheMiscMechanicIsDisabled() {
        PrepareSmithingEvent event = mock(PrepareSmithingEvent.class);
        SmithingInventory inventory = mock(SmithingInventory.class);
        ItemStack template = item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack input = item(Material.DIAMOND_SWORD);
        ItemStack material = item(Material.NETHERITE_INGOT);
        ItemStack vanillaBase = item(Material.DIAMOND_SWORD);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getInputTemplate()).thenReturn(template);
        when(inventory.getInputEquipment()).thenReturn(input);
        when(inventory.getInputMineral()).thenReturn(material);
        when(inventory.getContents()).thenReturn(new ItemStack[]{template, input, material});
        RecipeChoice matching = mock(RecipeChoice.class);
        when(matching.test(any())).thenReturn(true);
        when(matching.getItemStack()).thenReturn(vanillaBase);
        SmithingTransformRecipe first = mock(SmithingTransformRecipe.class), second = mock(SmithingTransformRecipe.class);
        for (SmithingTransformRecipe recipe : List.of(first, second)) {
            when(recipe.getTemplate()).thenReturn(matching);
            when(recipe.getBase()).thenReturn(matching);
            when(recipe.getAddition()).thenReturn(matching);
        }
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getName()).thenReturn("Oraxen");
        when(plugin.namespace()).thenReturn("oraxen");
        try (var plugins = mockStatic(OraxenPlugin.class, invocation -> invocation.getMethod().getReturnType() == OraxenPlugin.class
                     ? plugin : RETURNS_DEFAULTS.answer(invocation));
             var builders = mockStatic(RecipesBuilderEvents.class);
             var managers = mockStatic(RecipesEventsManager.class);
             var items = mockStatic(OraxenItems.class);
             var factories = mockStatic(MiscMechanicFactory.class);
             var bukkit = mockStatic(Bukkit.class)) {
            managers.when(RecipesEventsManager::get).thenReturn(mock(RecipesEventsManager.class));
            items.when(() -> OraxenItems.exists(input)).thenReturn(true);
            items.when(() -> OraxenItems.getIdByItem(input)).thenReturn("custom_sword");
            factories.when(MiscMechanicFactory::get).thenReturn(null);
            bukkit.when(Bukkit::recipeIterator).thenReturn(List.<Recipe>of(first, second).iterator());

            new SmithingRecipeEvents().onSmithingRecipe(event);

            verify(event).setResult(null);
        }
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        return item;
    }

    private void checkPreparedResult(boolean transform, boolean permitted, boolean changed) {
        PrepareSmithingEvent event = mock(PrepareSmithingEvent.class);
        SmithingInventory inventory = mock(SmithingInventory.class);
        Player player = mock(Player.class);
        ItemStack input = mock(ItemStack.class);
        when(input.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(input.getAmount()).thenReturn(1);
        RecipeChoice choice = mock(RecipeChoice.class);
        when(choice.test(input)).thenReturn(true);
        SmithingTransformRecipe recipe = mock(SmithingTransformRecipe.class);
        NamespacedKey key = NamespacedKey.minecraft("test_smithing");
        when(recipe.getKey()).thenReturn(key);
        when(recipe.getTemplate()).thenReturn(choice);
        when(recipe.getBase()).thenReturn(choice);
        when(recipe.getAddition()).thenReturn(choice);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getInputTemplate()).thenReturn(input);
        when(inventory.getInputEquipment()).thenReturn(input);
        when(inventory.getInputMineral()).thenReturn(input);
        SmithingTransformRecipe prepared = recipe;
        if (changed) {
            prepared = mock(SmithingTransformRecipe.class);
            when(prepared.getKey()).thenReturn(NamespacedKey.minecraft("other_smithing"));
        }
        when(inventory.getRecipe()).thenReturn(prepared);
        RecipesEventsManager manager = mock(RecipesEventsManager.class);
        when(manager.resolveSmithing(prepared, input, input, input)).thenReturn(recipe);
        when(manager.isConfiguredSmithingRecipe(key)).thenReturn(true);
        when(manager.hasSmithingPermission(player, key)).thenReturn(permitted);
        when(manager.matchesSmithing(key, input, input, input)).thenReturn(true);
        when(manager.hasSmithingTransform(key)).thenReturn(transform);
        ItemStack transformed = mock(ItemStack.class);
        when(manager.smithingResult(key, input, input, input, null)).thenReturn(transformed);
        try (var managers = mockStatic(RecipesEventsManager.class);
             var builders = mockStatic(RecipesBuilderEvents.class);
             var inventories = mockStatic(InventoryUtils.class)) {
            managers.when(RecipesEventsManager::get).thenReturn(manager);
            builders.when(() -> RecipesBuilderEvents.isBuilderInventory(event, SmithingBuilder.class)).thenReturn(false);
            inventories.when(() -> InventoryUtils.playerFromView(event)).thenReturn(player);
            new SmithingRecipeEvents().onSmithingRecipe(event);
            if (!permitted) verify(event).setResult(null);
            else if (transform || changed) verify(event).setResult(transformed);
            else verify(event, never()).setResult(any());
        }
    }
}
