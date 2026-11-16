package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
