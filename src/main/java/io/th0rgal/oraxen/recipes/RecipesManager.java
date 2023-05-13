package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.recipes.listeners.RecipesBuilderEvents;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.recipes.loaders.FurnaceLoader;
import io.th0rgal.oraxen.recipes.loaders.ShapedLoader;
import io.th0rgal.oraxen.recipes.loaders.ShapelessLoader;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

public class RecipesManager {

    private RecipesManager() {}

    public static void load(JavaPlugin plugin) {
        if (Settings.RESET_RECIPES.toBool()) {
            Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
            while (recipeIterator.hasNext()) {
                NamespacedKey recipeID = ((Keyed) recipeIterator.next()).getKey();
                if (recipeID.getNamespace().equals("oraxen")) {
                    Bukkit.removeRecipe(recipeID);
                }
            }
        }

        Bukkit.getPluginManager().registerEvents(new RecipesBuilderEvents(), plugin);
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                new ResourcesManager(plugin).extractConfigsInFolder("recipes", "yml");
            else try {
                new File(recipesFolder, "furnace.yml").createNewFile();
                new File(recipesFolder, "shaped.yml").createNewFile();
                new File(recipesFolder, "shapeless.yml").createNewFile();
            } catch (IOException e) {
                Logs.logError("Error while creating recipes files: " + e.getMessage());
            }
        }
        registerAllConfigRecipesFromFolder(recipesFolder);
        RecipesEventsManager.get().registerEvents();
    }

    public static void reload() {
        if (Settings.RESET_RECIPES.toBool())
            Bukkit.resetRecipes();
        RecipesEventsManager.get().resetRecipes();
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                new ResourcesManager(OraxenPlugin.get()).extractConfigsInFolder("recipes", "yml");
        }
        registerAllConfigRecipesFromFolder(recipesFolder);
        RecipesEventsManager.get().registerEvents();
    }

    private static void registerAllConfigRecipesFromFolder(File recipesFolder) {
        for (File configFile : Objects.requireNonNull(recipesFolder.listFiles()))
            registerConfigRecipes(configFile);
    }

    private static void registerConfigRecipes(File configFile) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String recipeSetting : config.getKeys(false)) {
            if (!config.isConfigurationSection(recipeSetting))
                continue;
            ConfigurationSection recipeSection = config.getConfigurationSection(recipeSetting);
            registerRecipeByType(configFile, recipeSection);
        }
    }

    private static void registerRecipeByType(File configFile, ConfigurationSection recipeSection) {
        try {
            switch (configFile.getName()) {
                case "shaped.yml" -> new ShapedLoader(recipeSection).registerRecipe();
                case "shapeless.yml" -> new ShapelessLoader(recipeSection).registerRecipe();
                case "furnace.yml" -> new FurnaceLoader(recipeSection).registerRecipe();
                default -> Logs.logError(configFile.getName());
            }
        } catch (NullPointerException exception) {
            Message.BAD_RECIPE.log(AdventureUtils.tagResolver("recipe", recipeSection.getName()));
        }
    }
}
