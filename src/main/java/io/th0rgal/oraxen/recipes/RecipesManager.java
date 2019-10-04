package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.ResourcesManager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class RecipesManager {

    public static void load(JavaPlugin plugin) {
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (recipesFolder.exists()) {
            recipesFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("recipes", "yml");
        }
        for (File configFile : recipesFolder.listFiles()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            for (String recipeSetting : config.getKeys(false)) {
                //todo: be able to support different kind of recipes
                new ShapedLoader(config.getConfigurationSection(recipeSetting)).registerRecipe();
            }
        }
    }

}