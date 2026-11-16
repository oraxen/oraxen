package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.RecipeTransforms;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipesEventsManagerTest {
    @Test
    void matcherPublicationIsAtomicAndRejectsSameMaterialPredicateFailures() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack accepted = item(), rejected = item();
        RecipeChoice choice = choice(accepted);
        ShapelessRecipe recipe = recipe("predicate", choice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice), -1, null);
        manager.finishRecipeReload();
        assertTrue(manager.prepareCrafter(recipe, matrix(accepted)));
        assertFalse(manager.prepareCrafter(recipe, matrix(rejected)));
        manager.resetRecipes();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice(rejected)), -1, null);
        assertTrue(manager.prepareCrafter(recipe, matrix(accepted)));
        manager.cancelRecipeReload();
        assertTrue(manager.prepareCrafter(recipe, matrix(accepted)));
    }

    @Test
    void broadServerChoicesSelectTheMatchingPaperBackedCustomRecipe() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack first = item(), second = item();
        when(first.getType()).thenReturn(Material.PAPER);
        when(second.getType()).thenReturn(Material.PAPER);
        RecipeChoice firstChoice = choice(first), secondChoice = choice(second);
        ShapelessRecipe firstRecipe = recipe("first_paper", firstChoice);
        ShapelessRecipe secondRecipe = recipe("second_paper", secondChoice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(firstRecipe.getKey(), List.of(firstChoice), -1, null);
        manager.registerShapelessRecipe(secondRecipe.getKey(), List.of(secondChoice), -1, null);
        manager.finishRecipeReload();
        assertTrue(manager.prepareCrafter(firstRecipe, matrix(first)));
        assertTrue(manager.prepareCrafter(secondRecipe, matrix(second)));
        try (var server = mockStatic(org.bukkit.Bukkit.class)) {
            server.when(() -> org.bukkit.Bukkit.getRecipe(firstRecipe.getKey())).thenReturn(firstRecipe);
            server.when(() -> org.bukkit.Bukkit.getRecipe(secondRecipe.getKey())).thenReturn(secondRecipe);
            assertTrue(manager.prepareCrafter(firstRecipe, matrix(second)));
            assertTrue(manager.prepareCrafter(secondRecipe, matrix(first)));
            assertSame(secondRecipe.getResult(), manager.craftingResult(firstRecipe, matrix(second), firstRecipe.getResult()));
        }
    }

    @Test
    void permissionsUseRecipeKeysAndCannotBeBypassedByAutomaticCrafters() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack ingredient = item();
        RecipeChoice choice = choice(ingredient);
        ShapelessRecipe locked = recipe("locked", choice), unlocked = recipe("unlocked", choice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(locked.getKey(), List.of(choice), -1, null);
        manager.registerShapelessRecipe(unlocked.getKey(), List.of(choice), -1, null);
        manager.addPermissionRecipe(CustomRecipe.fromRecipe(locked), "oraxen.recipe.locked");
        manager.finishRecipeReload();
        CommandSender sender = mock(CommandSender.class);
        assertFalse(manager.hasPermission(sender, CustomRecipe.fromRecipe(locked)));
        assertTrue(manager.hasPermission(sender, CustomRecipe.fromRecipe(unlocked)));
        assertFalse(manager.prepareCrafter(locked, matrix(ingredient)));
        assertTrue(manager.prepareCrafter(unlocked, matrix(ingredient)));
    }

    @Test
    void smithingUsesTheRegisteredPredicatesRatherThanTheServersMaterialChoices() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack template = item(), base = item(), addition = item(), rejected = item();
        NamespacedKey key = new NamespacedKey("oraxen", "smithing");
        manager.beginSmithingReload();
        manager.registerSmithingRecipe(key, null, choice(template), choice(base), choice(addition), null, "base");
        manager.finishSmithingReload();
        assertTrue(manager.matchesSmithing(key, template, base, addition));
        assertFalse(manager.matchesSmithing(key, template, rejected, addition));
        assertFalse(manager.matchesSmithing(key, rejected, base, addition));
        assertFalse(manager.matchesSmithing(key, template, base, rejected));
    }

    @Test
    void transformedShiftClicksCannotReuseTheFirstSourcesComponentsForBulkCrafts() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack ingredient = item();
        RecipeChoice choice = choice(ingredient);
        ShapelessRecipe recipe = recipe("transform", choice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice), 0, RecipeTransforms.keeping(List.of("enchantments")));
        manager.finishRecipeReload();
        CraftItemEvent event = mock(CraftItemEvent.class);
        CraftingInventory inventory = mock(CraftingInventory.class);
        when(event.getRecipe()).thenReturn(recipe);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getMatrix()).thenReturn(matrix(ingredient));
        when(event.isShiftClick()).thenReturn(true);
        manager.onCraftItem(event);
        verify(event).setCancelled(true);
    }

    private ShapelessRecipe recipe(String name, RecipeChoice choice) {
        ShapelessRecipe recipe = mock(ShapelessRecipe.class);
        when(recipe.getKey()).thenReturn(new NamespacedKey("oraxen", name));
        when(recipe.getChoiceList()).thenReturn(List.of(choice));
        ItemStack result = item();
        when(recipe.getResult()).thenReturn(result);
        return recipe;
    }

    private RecipeChoice choice(ItemStack accepted) {
        RecipeChoice choice = mock(RecipeChoice.class);
        when(choice.clone()).thenReturn(choice);
        when(choice.getItemStack()).thenReturn(accepted);
        when(choice.test(any())).thenAnswer(invocation -> invocation.getArgument(0) == accepted);
        return choice;
    }

    private ItemStack item() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        return item;
    }

    private ItemStack[] matrix(ItemStack item) {
        ItemStack[] matrix = new ItemStack[9];
        matrix[8] = item;
        return matrix;
    }
}
