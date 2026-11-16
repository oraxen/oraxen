package io.th0rgal.oraxen.recipes;

import io.papermc.paper.potion.PotionMix;
import io.th0rgal.oraxen.recipes.loaders.BrewingLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.potion.PotionBrewer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecipeBrewingTest {
    @Test
    void testsEachBottleWithoutMutatingTheReagent() {
        ItemStack bottle = item(Material.POTION);
        ItemStack other = item(Material.SPLASH_POTION);
        ItemStack reagent = item(Material.REDSTONE);
        RecipeBrewing recipe = recipe("one");

        assertTrue(recipe.matches(bottle, reagent));
        assertFalse(recipe.matches(other, reagent));
        assertTrue(recipe.matches(bottle, reagent));
        assertFalse(recipe.matches(bottle, null));
        verify(reagent, never()).setAmount(anyInt());
        verify(bottle, never()).setAmount(anyInt());
    }

    @Test
    void reloadAndClearOnlyRemoveOwnedKeysAndCancellationKeepsLiveRecipes() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PotionBrewer brewer = mock(PotionBrewer.class);
            bukkit.when(Bukkit::getPotionBrewer).thenReturn(brewer);
            List<NamespacedKey> removed = new ArrayList<>();
            doAnswer(call -> { removed.add(call.getArgument(0)); return null; })
                    .when(brewer).removePotionMix(any());
            try {
                BrewingLoader.beginReload();
                BrewingLoader.register(recipe("first"));
                BrewingLoader.validateReload();
                verify(brewer, never()).addPotionMix(any());
                assertTrue(removed.isEmpty());
                BrewingLoader.finishReload();
                BrewingLoader.beginReload();
                BrewingLoader.register(recipe("cancelled"));
                BrewingLoader.cancelReload();
                assertTrue(removed.isEmpty());

                BrewingLoader.beginReload();
                BrewingLoader.register(recipe("second"));
                BrewingLoader.finishReload();
                BrewingLoader.clear();
                assertEquals(List.of(key("first"), key("second")), removed);
                verify(brewer, times(2)).addPotionMix(any());
                verify(brewer, never()).resetPotionMixes();
            } finally {
                BrewingLoader.clear();
            }
        }
    }

    @Test
    void rejectedReloadRestoresPreviousMixesAndRemovesPartiallyAddedMixes() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PotionBrewer brewer = mock(PotionBrewer.class);
            bukkit.when(Bukkit::getPotionBrewer).thenReturn(brewer);
            Map<NamespacedKey, PotionMix> live = new LinkedHashMap<>();
            doAnswer(call -> {
                PotionMix mix = call.getArgument(0);
                if (mix.getKey().equals(key("rejected"))) throw new IllegalArgumentException("rejected");
                live.put(mix.getKey(), mix);
                return null;
            }).when(brewer).addPotionMix(any());
            doAnswer(call -> { live.remove(call.getArgument(0)); return null; })
                    .when(brewer).removePotionMix(any());
            try {
                BrewingLoader.beginReload();
                BrewingLoader.register(recipe("original"));
                BrewingLoader.finishReload();
                BrewingLoader.beginReload();
                BrewingLoader.register(recipe("partial"));
                BrewingLoader.register(recipe("rejected"));
                assertThrows(IllegalArgumentException.class, BrewingLoader::finishReload);
                assertEquals(List.of(key("original")), new ArrayList<>(live.keySet()));
            } finally {
                BrewingLoader.clear();
            }
        }
    }

    private static NamespacedKey key(String name) { return new NamespacedKey("oraxen", name); }

    private static RecipeBrewing recipe(String name) {
        return new RecipeBrewing(key(name), PotionMix.createPredicateChoice(item -> item.getType() == Material.POTION),
                PotionMix.createPredicateChoice(item -> item.getType() == Material.REDSTONE), item(Material.DIAMOND));
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        return item;
    }
}
