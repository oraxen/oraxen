package io.th0rgal.oraxen.recipes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomWorkstationRecipeTest {

    @AfterEach
    void publishEmptyRegistry() {
        CustomWorkstationRegistry.beginReload();
        CustomWorkstationRegistry.finishReload();
    }

    @Test
    void ingredientRequiresChoiceAndConfiguredAmount() {
        CustomWorkstationRecipe.Ingredient ingredient = new CustomWorkstationRecipe.Ingredient(
                choice(Material.DIAMOND), 2);

        assertTrue(ingredient.matches(item(Material.DIAMOND, 2)));
        assertFalse(ingredient.matches(item(Material.DIAMOND, 1)));
        assertFalse(ingredient.matches(item(Material.EMERALD, 2)));
        assertFalse(ingredient.matches(null));
    }

    @Test
    void structuralMatchRequiresEmptySecondSlotForOneInputRecipe() {
        CustomWorkstationRecipe recipe = recipe(null);

        assertTrue(recipe.matches(item(Material.DIAMOND, 1), null));
        assertFalse(recipe.matches(item(Material.DIAMOND, 1), item(Material.STICK, 1)));
    }

    @Test
    void registryStructuralMatchingDoesNotConsiderPermission() {
        CustomWorkstationRecipe recipe = recipe("oraxen.restricted");
        CustomWorkstationRegistry.beginReload();
        CustomWorkstationRegistry.register(recipe);
        CustomWorkstationRegistry.finishReload();

        assertSame(recipe, CustomWorkstationRegistry.match(CustomWorkstationRecipe.Type.GRINDSTONE,
                item(Material.DIAMOND, 1), null));
    }

    private CustomWorkstationRecipe recipe(String permission) {
        return new CustomWorkstationRecipe("test", CustomWorkstationRecipe.Type.GRINDSTONE,
                new CustomWorkstationRecipe.Ingredient(choice(Material.DIAMOND), 1),
                null, item(Material.EMERALD, 1), 0, permission, null, 1);
    }

    private RecipeChoice choice(Material material) {
        RecipeChoice choice = mock(RecipeChoice.class);
        when(choice.test(any(ItemStack.class))).thenAnswer(invocation ->
                invocation.<ItemStack>getArgument(0).getType() == material);
        return choice;
    }

    private ItemStack item(Material material, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(amount);
        when(item.isEmpty()).thenReturn(false);
        return item;
    }
}
