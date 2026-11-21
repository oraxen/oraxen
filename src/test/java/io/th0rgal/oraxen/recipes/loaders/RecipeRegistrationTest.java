package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.RecipeIngredients;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RecipeRegistrationTest {
    @AfterEach
    void cancelPass() {
        RecipeLoader.cancelRegistrationPass();
    }

    @Test
    void cancelledPassDoesNotChangeRegisteredRecipes() {
        Recipe replacement = recipe("cancelled");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            RecipeLoader.beginRegistrationPass();
            loader(replacement).registerRecipe();
            RecipeLoader.cancelRegistrationPass();
            bukkit.verifyNoInteractions();
        }
    }

    @Test
    void rejectedRegistrationRestoresPreviousRecipe() {
        Recipe replacement = recipe("rollback");
        Recipe previous = recipe("rollback");
        NamespacedKey key = ((Keyed) replacement).getKey();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getRecipe(key)).thenReturn(previous);
            bukkit.when(() -> Bukkit.addRecipe(replacement)).thenReturn(false);
            bukkit.when(() -> Bukkit.addRecipe(previous)).thenReturn(true);
            RecipeLoader.beginRegistrationPass();
            loader(replacement).registerRecipe();
            bukkit.verify(() -> Bukkit.addRecipe(replacement), never());
            assertThrows(IllegalStateException.class, RecipeLoader::finishRegistrationPass);
            bukkit.verify(() -> Bukkit.addRecipe(previous));
        }
    }

    @Test
    void laterReloadFailureRollsBackAppliedNativeRecipesAndTags() {
        Recipe replacement = recipe("later_failure");
        Recipe previous = recipe("later_failure");
        NamespacedKey key = ((Keyed) replacement).getKey();
        org.bukkit.inventory.RecipeChoice tag = mock(org.bukkit.inventory.RecipeChoice.class);
        when(tag.clone()).thenReturn(tag);
        io.th0rgal.oraxen.recipes.RecipeIngredients.publishTags(java.util.Map.of("oraxen:old", tag));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getRecipe(key)).thenReturn(previous);
            bukkit.when(() -> Bukkit.addRecipe(replacement)).thenReturn(true);
            bukkit.when(() -> Bukkit.addRecipe(previous)).thenReturn(true);
            RecipeLoader.beginRegistrationPass();
            io.th0rgal.oraxen.recipes.RecipeIngredients.load(null);
            loader(replacement).registerRecipe();
            RecipeLoader.finishRegistrationPass();
            RecipeLoader.cancelRegistrationPass();
            bukkit.verify(() -> Bukkit.addRecipe(previous));
            org.junit.jupiter.api.Assertions.assertTrue(io.th0rgal.oraxen.recipes.RecipeIngredients.tags().containsKey("oraxen:old"));
        } finally {
            io.th0rgal.oraxen.recipes.RecipeIngredients.load(null);
        }
    }

    @Test
    void malformedTransformsLeaveThePreviousRecipeActive() {
        Recipe previous = recipe("malformed_transform");
        NamespacedKey key = ((Keyed) previous).getKey();
        for (Object malformed : java.util.List.of(java.util.List.of(), "keep-enchantments", 42)) {
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getRecipe(key)).thenReturn(previous);
                RecipeLoader.beginRegistrationPass();
                YamlConfiguration configuration = new YamlConfiguration();
                configuration.set("transform", malformed);
                assertThrows(IllegalArgumentException.class,
                        () -> io.th0rgal.oraxen.recipes.RecipeTransforms.parse(configuration));
                RecipeLoader.cancelRegistrationPass();
                org.junit.jupiter.api.Assertions.assertSame(previous, Bukkit.getRecipe(key));
                bukkit.verify(() -> Bukkit.removeRecipe(any()), never());
                bukkit.verify(() -> Bukkit.addRecipe(any()), never());
            }
        }
    }

    @Test
    void resetSettingControlsWhetherDeletedRecipesSurviveReload() {
        for (boolean reset : new boolean[]{false, true}) {
            Recipe previous = recipe("deleted_on_reload");
            NamespacedKey key = ((Keyed) previous).getKey();
            java.util.Map<NamespacedKey, Recipe> registry = new java.util.HashMap<>();
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getRecipe(any())).thenAnswer(call -> registry.get(call.getArgument(0)));
                bukkit.when(() -> Bukkit.removeRecipe(any())).thenAnswer(call -> registry.remove(call.getArgument(0)) != null);
                bukkit.when(() -> Bukkit.addRecipe(any())).thenAnswer(call -> {
                    Recipe recipe = call.getArgument(0);
                    return registry.putIfAbsent(((Keyed) recipe).getKey(), recipe) == null;
                });
                RecipeLoader.beginRegistrationPass(true);
                loader(previous).registerRecipe();
                RecipeLoader.finishRegistrationPass();
                RecipeLoader.completeRegistrationPass();
                Recipe replacement = recipe("deleted_on_reload");
                RecipeLoader.beginRegistrationPass(reset);
                loader(replacement).registerRecipe();
                RecipeLoader.finishRegistrationPass();
                org.junit.jupiter.api.Assertions.assertSame(replacement, registry.get(key));
                RecipeLoader.cancelRegistrationPass();
                org.junit.jupiter.api.Assertions.assertSame(previous, registry.get(key));
                RecipeLoader.beginRegistrationPass(reset);
                RecipeLoader.finishRegistrationPass();
                org.junit.jupiter.api.Assertions.assertEquals(!reset, registry.containsKey(key));
                RecipeLoader.cancelRegistrationPass();
                org.junit.jupiter.api.Assertions.assertSame(previous, registry.get(key));
                RecipeLoader.beginRegistrationPass(reset);
                RecipeLoader.finishRegistrationPass();
                RecipeLoader.completeRegistrationPass();
                org.junit.jupiter.api.Assertions.assertEquals(!reset, registry.containsKey(key));
            }
        }
    }

    @Test
    void ingredientDisplaysFollowTheRegisteredRecipeOnReload() {
        NamespacedKey key = new NamespacedKey("oraxen", "backpack_upgrade");
        ItemStack backpack = mock(ItemStack.class);
        when(backpack.clone()).thenReturn(backpack);
        when(backpack.getType()).thenReturn(Material.PAPER);
        when(backpack.hasItemMeta()).thenReturn(true);
        ItemBuilder builder = mock(ItemBuilder.class);
        when(builder.build()).thenReturn(backpack);
        Map<NamespacedKey, Recipe> registry = new HashMap<>();
        java.util.concurrent.atomic.AtomicBoolean customDisplay = new java.util.concurrent.atomic.AtomicBoolean(true);
        java.util.concurrent.atomic.AtomicBoolean rejectReplacement = new java.util.concurrent.atomic.AtomicBoolean();
        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            items.when(() -> OraxenItems.getItemById("iron_backpack")).thenReturn(builder);
            bukkit.when(() -> Bukkit.getRecipe(any())).thenAnswer(call -> registry.get(call.getArgument(0)));
            bukkit.when(() -> Bukkit.removeRecipe(any())).thenAnswer(call -> registry.remove(call.getArgument(0)) != null);
            bukkit.when(() -> Bukkit.addRecipe(any())).thenAnswer(call -> {
                Recipe recipe = call.getArgument(0);
                if (rejectReplacement.getAndSet(false)) {
                    assertTrue(RecipeLoader.ingredientDisplays(key).isEmpty());
                    customDisplay.set(true);
                    return false;
                }
                if (customDisplay.get())
                    assertSame(backpack, RecipeLoader.ingredientDisplays(key).get(0).getFirst());
                else assertTrue(RecipeLoader.ingredientDisplays(key).isEmpty());
                return registry.putIfAbsent(((Keyed) recipe).getKey(), recipe) == null;
            });

            RecipeLoader.beginRegistrationPass();
            loader(new ShapelessRecipe(key, new ItemStack(Material.GOLD_INGOT))
                    .addIngredient(RecipeIngredients.choice("oraxen:iron_backpack"))).registerRecipe();
            RecipeLoader.finishRegistrationPass();
            RecipeLoader.completeRegistrationPass();

            RecipeLoader.beginRegistrationPass(false);
            RecipeLoader.finishRegistrationPass();
            RecipeLoader.completeRegistrationPass();
            assertSame(backpack, RecipeLoader.ingredientDisplays(key).get(0).getFirst());

            RecipeLoader.beginRegistrationPass(false);
            loader(new ShapelessRecipe(key, new ItemStack(Material.GOLD_INGOT))
                    .addIngredient(Material.PAPER)).registerRecipe();
            rejectReplacement.set(true);
            assertThrows(IllegalStateException.class, RecipeLoader::finishRegistrationPass);
            assertSame(backpack, RecipeLoader.ingredientDisplays(key).get(0).getFirst());
            RecipeLoader.cancelRegistrationPass();

            RecipeLoader.beginRegistrationPass(false);
            loader(new ShapelessRecipe(key, new ItemStack(Material.GOLD_INGOT))
                    .addIngredient(Material.PAPER)).registerRecipe();
            customDisplay.set(false);
            RecipeLoader.finishRegistrationPass();
            assertTrue(RecipeLoader.ingredientDisplays(key).isEmpty());
            customDisplay.set(true);
            RecipeLoader.cancelRegistrationPass();
            assertSame(backpack, RecipeLoader.ingredientDisplays(key).get(0).getFirst());

            RecipeLoader.beginRegistrationPass();
            RecipeLoader.finishRegistrationPass();
            RecipeLoader.completeRegistrationPass();
            assertTrue(RecipeLoader.ingredientDisplays(key).isEmpty());
        }
    }

    @Test
    void duplicateKeysFailBeforeRegistryMutation() {
        Recipe recipe = recipe("duplicate");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            RecipeLoader.beginRegistrationPass();
            loader(recipe).registerRecipe();
            assertThrows(IllegalArgumentException.class, () -> loader(recipe).registerRecipe());
            bukkit.verifyNoInteractions();
        }
    }

    private static Recipe recipe(String name) {
        Recipe recipe = mock(Recipe.class, withSettings().extraInterfaces(Keyed.class));
        when(((Keyed) recipe).getKey()).thenReturn(NamespacedKey.minecraft(name));
        return recipe;
    }

    private static RecipeLoader loader(Recipe recipe) {
        return new RecipeLoader(new YamlConfiguration()) {
            @Override
            public void registerRecipe() {
                loadRecipe(recipe);
            }
        };
    }
}
