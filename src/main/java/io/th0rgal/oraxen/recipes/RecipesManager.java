package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.recipes.listeners.RecipesBuilderEvents;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.recipes.loaders.ShapedLoader;
import io.th0rgal.oraxen.recipes.loaders.ShapelessLoader;
import io.th0rgal.oraxen.settings.ResourcesManager;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class RecipesManager {

    public static void load(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(new RecipesBuilderEvents(), plugin);
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("recipes", "yml");
        }
        registerAllConfigRecipesFromFolder(recipesFolder);
        RecipesEventsManager.get().registerEvents();
    }
    
    private static void registerAllConfigRecipesFromFolder(File recipesFolder) {
        for (File configFile : recipesFolder.listFiles()) {
            registerConfigRecipes(configFile);
        }
    }

    private static void registerConfigRecipes(File configFile) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String recipeSetting : config.getKeys(false)) {
            ConfigurationSection recipeSection = config.getConfigurationSection(recipeSetting);
            registerRecipeByType(configFile, recipeSection);
        }
    }

    private static void registerRecipeByType(File configFile, ConfigurationSection recipeSection) {
        switch (configFile.getName()) {
            case "shaped.yml":
                new ShapedLoader(recipeSection).registerRecipe();
                break;
            case "shapeless.yml":
                new ShapelessLoader(recipeSection).registerRecipe();
                break;
            default:
                Logs.logError(configFile.getName());
                break;
        }
    }
}
