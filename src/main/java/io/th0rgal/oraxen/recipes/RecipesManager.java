package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.recipes.loaders.ShapedLoader;
import io.th0rgal.oraxen.recipes.loaders.ShapelessLoader;
import io.th0rgal.oraxen.settings.ResourcesManager;

import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class RecipesManager {

    public static void load(JavaPlugin plugin) {
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            new ResourcesManager(plugin).extractConfigsInFolder("recipes", "yml");
        }
        for (File configFile : recipesFolder.listFiles()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            for (String recipeSetting : config.getKeys(false)) {

                switch (configFile.getName()) {
                    case "shaped.yml":
                        new ShapedLoader(config.getConfigurationSection(recipeSetting)).registerRecipe();
                        break;
                    case "shapeless.yml":
                        new ShapelessLoader(config.getConfigurationSection(recipeSetting)).registerRecipe();
                        break;
                    default:
                        Logs.logError(configFile.getName());
                }


            }
        }
    }

}