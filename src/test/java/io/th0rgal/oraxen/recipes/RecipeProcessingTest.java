package io.th0rgal.oraxen.recipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecuttingRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeProcessingTest {
    @BeforeEach
    @AfterEach
    void clearRegisteredChoices() {
        RecipeProcessing.beginReload();
        RecipeProcessing.finishReload();
    }

    @Test
    void overlappingCookingRecipesResolveTheOriginalIngredientMatchers() {
        CookingRecipe<?> first = cooking("first"), second = cooking("second");
        RecipeProcessing.beginReload();
        RecipeProcessing.register(first, new InputChoice(Material.IRON_ORE));
        RecipeProcessing.register(second, new InputChoice(Material.GOLD_ORE));
        RecipeProcessing.finishReload();
        try (var server = mockStatic(org.bukkit.Bukkit.class)) {
            server.when(() -> org.bukkit.Bukkit.getRecipe(second.getKey())).thenReturn(second);
            assertSame(second, RecipeProcessing.resolve(first, item(Material.GOLD_ORE)));
            assertSame(first, RecipeProcessing.resolve(first, item(Material.IRON_ORE)));
            assertNull(RecipeProcessing.resolve(first, item(Material.DIRT)));
        }
    }

    @Test
    void cookingAndStonecuttingUseTheSelectedRecipesOwnIngredientChoice() {
        CookingRecipe<?> cooking = cooking("smelt_iron");
        StonecuttingRecipe cutting = cutting("cut_stone");
        RecipeProcessing.beginReload();
        RecipeProcessing.register(cooking, new InputChoice(Material.IRON_ORE));
        RecipeProcessing.register(cutting, new InputChoice(Material.STONE));
        RecipeProcessing.finishReload();

        ItemStack ore = item(Material.IRON_ORE);
        ItemStack stone = item(Material.STONE);
        assertTrue(RecipeProcessing.matches(cooking, ore));
        assertFalse(RecipeProcessing.matches(cooking, stone));
        assertFalse(RecipeProcessing.matches(cooking, null));
        assertTrue(RecipeProcessing.matches(cutting, stone));
        assertFalse(RecipeProcessing.matches(cutting, ore));
        verify(ore, never()).setAmount(anyInt());
        verify(stone, never()).setAmount(anyInt());
    }

    @Test
    void stagedChoicesStayInvisibleAndCancellationKeepsThePreviousLiveChoices() {
        CookingRecipe<?> existing = cooking("existing");
        StonecuttingRecipe added = cutting("new_recipe");
        RecipeProcessing.beginReload();
        RecipeProcessing.register(existing, new InputChoice(Material.IRON_ORE));
        RecipeProcessing.finishReload();

        RecipeProcessing.beginReload();
        RecipeProcessing.register(existing, new InputChoice(Material.GOLD_ORE));
        RecipeProcessing.register(added, new InputChoice(Material.STONE));
        assertTrue(RecipeProcessing.matches(existing, item(Material.IRON_ORE)));
        assertFalse(RecipeProcessing.matches(existing, item(Material.GOLD_ORE)));
        assertTrue(RecipeProcessing.matches(added, item(Material.DIRT)));

        RecipeProcessing.cancelReload();
        assertTrue(RecipeProcessing.matches(existing, item(Material.IRON_ORE)));
        assertFalse(RecipeProcessing.matches(existing, item(Material.GOLD_ORE)));
        assertTrue(RecipeProcessing.matches(added, item(Material.DIRT)));
    }

    @Test
    void completedReloadReplacesChoicesAndRemovesDeletedRecipeRestrictions() {
        CookingRecipe<?> updated = cooking("updated");
        StonecuttingRecipe removed = cutting("removed");
        RecipeProcessing.beginReload();
        RecipeProcessing.register(updated, new InputChoice(Material.IRON_ORE));
        RecipeProcessing.register(removed, new InputChoice(Material.STONE));
        RecipeProcessing.finishReload();
        assertFalse(RecipeProcessing.matches(removed, item(Material.DIRT)));

        RecipeProcessing.beginReload();
        RecipeProcessing.register(updated, new InputChoice(Material.GOLD_ORE));
        RecipeProcessing.finishReload();
        assertTrue(RecipeProcessing.matches(updated, item(Material.GOLD_ORE)));
        assertFalse(RecipeProcessing.matches(updated, item(Material.IRON_ORE)));
        assertTrue(RecipeProcessing.matches(removed, item(Material.DIRT)));
    }

    @Test
    void registrationSnapshotsTheChoiceRatherThanRetainingTheCallersMutableMatcher() {
        CookingRecipe<?> recipe = cooking("snapshot");
        RecipeChoice callerChoice = mock(RecipeChoice.class);
        when(callerChoice.clone()).thenReturn(new InputChoice(Material.IRON_ORE));
        RecipeProcessing.beginReload();
        RecipeProcessing.register(recipe, callerChoice);
        RecipeProcessing.finishReload();

        assertTrue(RecipeProcessing.matches(recipe, item(Material.IRON_ORE)));
        assertFalse(RecipeProcessing.matches(recipe, item(Material.DIRT)));
        verify(callerChoice).clone();
        verify(callerChoice, never()).test(any());
    }

    @Test
    void recipesOutsideTheRegistryAreNotRestricted() {
        assertTrue(RecipeProcessing.matches(cooking("vanilla_recipe"), item(Material.DIRT)));
        assertTrue(RecipeProcessing.matches(mock(Recipe.class), item(Material.DIRT)));
    }

    private CookingRecipe<?> cooking(String name) {
        CookingRecipe<?> recipe = mock(CookingRecipe.class);
        when(recipe.getKey()).thenReturn(new NamespacedKey("oraxen", name));
        return recipe;
    }

    private StonecuttingRecipe cutting(String name) {
        StonecuttingRecipe recipe = mock(StonecuttingRecipe.class);
        when(recipe.getKey()).thenReturn(new NamespacedKey("oraxen", name));
        return recipe;
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        return item;
    }

    private record InputChoice(Material material) implements RecipeChoice {
        @Override
        public ItemStack getItemStack() {
            return item(material);
        }

        @Override
        public boolean test(ItemStack input) {
            return input != null && input.getType() == material;
        }

        @Override
        public InputChoice clone() {
            return new InputChoice(material);
        }
    }

}
