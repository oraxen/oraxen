package io.th0rgal.oraxen.recipes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeCraftingTest {
    @Test
    void shapedMatchesFullGridAtOffsetsAndWhenMirrored() {
        ItemStack source = item(), other = item(), extra = item();
        RecipeCrafting.Matcher matcher = RecipeCrafting.shaped(List.of("AB"),
                Map.of('A', choice(stack -> stack == source), 'B', choice(stack -> stack == other)), 'A');
        ItemStack[] matrix = new ItemStack[9];
        matrix[7] = other;
        matrix[8] = source;
        assertSame(source, matcher.match(matrix).source());
        matrix[0] = extra;
        assertNull(matcher.match(matrix));
        matrix[0] = null;
        matrix[7] = extra;
        assertNull(matcher.match(matrix));
    }

    @Test
    void emptyShapeMarginsDoNotPreventOffsetsOrSmallCraftingGrids() {
        ItemStack ingredient = item();
        RecipeCrafting.Matcher matcher = RecipeCrafting.shaped(List.of("   ", " A ", "   "),
                Map.of('A', choice(stack -> stack == ingredient)), 'A');
        assertSame(ingredient, matcher.match(new ItemStack[]{ingredient, null, null, null}).source());
    }

    @Test
    void shapedChecksEmptySlotsAndEveryIngredientPredicate() {
        ItemStack source = item(), rejected = item(), other = item();
        RecipeCrafting.Matcher matcher = RecipeCrafting.shaped(List.of("A B"),
                Map.of('A', choice(stack -> stack == source), 'B', choice(stack -> stack == other)), 'A');
        ItemStack[] matrix = {source, null, other, null, null, null, null, null, null};
        assertNotNull(matcher.match(matrix));
        matrix[1] = rejected;
        assertNull(matcher.match(matrix));
        matrix[1] = null;
        matrix[2] = rejected;
        assertNull(matcher.match(matrix));
    }

    @Test
    void shapelessBacktracksOverOverlappingAlternativesAndKeepsSourceAssignment() {
        ItemStack narrow = item(), broadOnly = item();
        RecipeCrafting.Matcher matcher = RecipeCrafting.shapeless(List.of(
                choice(stack -> stack == narrow || stack == broadOnly), choice(stack -> stack == narrow)), 1);
        ItemStack[] matrix = {narrow, broadOnly, null, null};
        assertSame(narrow, matcher.match(matrix).source());
        matrix[2] = item();
        assertNull(matcher.match(matrix));
        matrix[2] = null;
        matrix[1] = null;
        assertNull(matcher.match(matrix));
    }

    @Test
    void duplicateIngredientsRequireSeparateSlotsNotStackAmounts() {
        ItemStack ingredient = item();
        when(ingredient.getAmount()).thenReturn(64);
        RecipeChoice choice = choice(stack -> stack == ingredient);
        RecipeCrafting.Matcher matcher = RecipeCrafting.shapeless(List.of(choice, choice), -1);
        assertNull(matcher.match(new ItemStack[]{ingredient, null, null, null}));
        assertNotNull(matcher.match(new ItemStack[]{ingredient, ingredient, null, null}));
    }

    @Test
    void ambiguousRepeatedTransformSourceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RecipeCrafting.shaped(List.of("AA"),
                Map.of('A', choice(stack -> true)), 'A'));
    }

    private ItemStack item() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(item.getAmount()).thenReturn(1);
        return item;
    }

    private RecipeChoice choice(Predicate<ItemStack> predicate) {
        RecipeChoice choice = mock(RecipeChoice.class);
        when(choice.clone()).thenReturn(choice);
        when(choice.test(any())).thenAnswer(invocation -> predicate.test(invocation.getArgument(0)));
        return choice;
    }
}
