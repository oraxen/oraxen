package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RecipeLoaderTest {

    @Test
    void ignoresMmoItemsWhenPluginIsDisabled() {
        TestRecipeLoader loader = new TestRecipeLoader(recipeSection());
        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.isPluginEnabled("MMOItems")).thenReturn(false);

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            assertAll(
                    () -> assertNull(loader.result()),
                    () -> assertNull(loader.ingredient()),
                    () -> assertNull(loader.choice()));
        }
    }

    private static ConfigurationSection recipeSection() {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection recipe = configuration.createSection("test");
        ConfigurationSection result = recipe.createSection("result");
        result.set("mmoitems_id", "test_item");
        result.set("mmoitems_type", "MATERIAL");
        ConfigurationSection ingredient = recipe.createSection("ingredient");
        ingredient.set("mmoitems_id", "test_item");
        ingredient.set("mmoitems_type", "MATERIAL");
        return recipe;
    }

    private static final class TestRecipeLoader extends RecipeLoader {

        private TestRecipeLoader(ConfigurationSection section) {
            super(section);
        }

        private ItemStack result() {
            return getResult();
        }

        private ItemStack ingredient() {
            return getIndredientItemStack(getSection().getConfigurationSection("ingredient"));
        }

        private RecipeChoice choice() {
            return getRecipeChoice(getSection().getConfigurationSection("ingredient"));
        }

        @Override
        public void registerRecipe() {
        }
    }
}
