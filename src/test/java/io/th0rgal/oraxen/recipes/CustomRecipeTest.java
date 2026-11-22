package io.th0rgal.oraxen.recipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomRecipeTest {
    private final ItemStack result = item(Material.DIAMOND);

    @Test
    void shapedRecipesWithTheSameResultButDifferentHeightsAreDifferent() {
        CustomRecipe twoRows = CustomRecipe.fromRecipe(shaped("two_rows", "SS", "SS"));
        CustomRecipe threeRows = CustomRecipe.fromRecipe(shaped("three_rows", "SS", "SS", "SS"));

        assertNotEquals(twoRows, threeRows);
        assertNotEquals(threeRows, twoRows);
        assertTrue(Set.copyOf(List.of(twoRows, threeRows)).contains(threeRows));
    }

    @Test
    void shapelessRecipesCompareIngredientCountsInAnyOrder() {
        CustomRecipe stoneDirt = shapeless("stone_dirt", Material.STONE, Material.DIRT);
        CustomRecipe dirtStone = shapeless("dirt_stone", Material.DIRT, Material.STONE);

        assertEquals(stoneDirt, dirtStone);
        assertEquals(stoneDirt.hashCode(), dirtStone.hashCode());
        for (CustomRecipe other : List.of(shapeless("stone", Material.STONE),
                shapeless("stone_stone", Material.STONE, Material.STONE))) {
            assertNotEquals(stoneDirt, other);
            assertNotEquals(other, stoneDirt);
        }
    }

    private ShapedRecipe shaped(String name, String... shape) {
        Map<Character, RecipeChoice> choices = Map.of('S', choice(Material.STONE));
        ShapedRecipe recipe = mock(ShapedRecipe.class);
        when(recipe.getKey()).thenReturn(NamespacedKey.minecraft(name));
        when(recipe.getResult()).thenReturn(result);
        when(recipe.getShape()).thenReturn(shape);
        when(recipe.getChoiceMap()).thenReturn(choices);
        return recipe;
    }

    private CustomRecipe shapeless(String name, Material... ingredients) {
        List<RecipeChoice> choices = Arrays.stream(ingredients).map(CustomRecipeTest::choice).toList();
        ShapelessRecipe recipe = mock(ShapelessRecipe.class);
        when(recipe.getKey()).thenReturn(NamespacedKey.minecraft(name));
        when(recipe.getResult()).thenReturn(result);
        when(recipe.getChoiceList()).thenReturn(choices);
        return CustomRecipe.fromRecipe(recipe);
    }

    private static RecipeChoice choice(Material material) {
        RecipeChoice choice = mock(RecipeChoice.class);
        ItemStack item = item(material);
        when(choice.getItemStack()).thenReturn(item);
        return choice;
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.clone()).thenReturn(item);
        when(item.isSimilar(any())).thenAnswer(call -> call.getArgument(0) instanceof ItemStack other && other.getType() == material);
        return item;
    }
}
