package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.recipes.RecipeIngredients;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        AtomicBoolean customDisplay = new AtomicBoolean(true);
        AtomicBoolean rejectReplacement = new AtomicBoolean();
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

    @Test
    void duplicateRecipeNamesKeepTheFirstRecipesIngredientMatcher() {
        RecipesEventsManager events = new RecipesEventsManager();
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getName()).thenReturn("Oraxen");
        when(plugin.namespace()).thenReturn("oraxen");
        ItemStack stone = mock(ItemStack.class), dirt = mock(ItemStack.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(dirt.getType()).thenReturn(Material.DIRT);
        when(stone.getAmount()).thenReturn(1);
        when(dirt.getAmount()).thenReturn(1);
        ShapelessRecipe crafted = mock(ShapelessRecipe.class);
        when(crafted.getKey()).thenReturn(new NamespacedKey("oraxen", "duplicate"));
        try (MockedStatic<OraxenPlugin> plugins = mockStatic(OraxenPlugin.class);
             MockedStatic<RecipesEventsManager> managers = mockStatic(RecipesEventsManager.class, CALLS_REAL_METHODS);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            managers.when(RecipesEventsManager::get).thenReturn(events);
            RecipeLoader.beginRegistrationPass();
            events.resetRecipes();
            new ShapelessLoader(shapeless("minecraft:stone")).registerRecipe();
            assertThrows(IllegalArgumentException.class, () -> new ShapelessLoader(shapeless("minecraft:dirt")).registerRecipe());
            events.finishRecipeReload();

            assertTrue(events.prepareCrafter(crafted, matrix(stone)));
            assertFalse(events.prepareCrafter(crafted, matrix(dirt)));
        }
    }

    private static ConfigurationSection shapeless(String ingredient) {
        ConfigurationSection section = new YamlConfiguration().createSection("duplicate");
        section.set("result", "minecraft:diamond");
        section.set("ingredients", List.of(ingredient));
        return section;
    }

    private static ItemStack[] matrix(ItemStack item) {
        ItemStack[] matrix = new ItemStack[9];
        matrix[4] = item;
        return matrix;
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
