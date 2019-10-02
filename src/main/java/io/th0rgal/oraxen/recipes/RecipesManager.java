package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.settings.ResourcesManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RecipesManager {

    private static Map<String, RecipeConfig> recipeConfigMap = new HashMap<>();

    public static void load(JavaPlugin plugin) {
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (recipesFolder.exists())
            recipesFolder.mkdirs();
        new ResourcesManager(plugin).extractConfigsInFolder("recipes", "yml");
        for (File configFile : recipesFolder.listFiles()) {

            RecipeConfig recipeConfig = new RecipeConfig(plugin, configFile);
            recipeConfigMap.put(recipeConfig.getName(), recipeConfig);



        }
    }

}

class RecipeConfig {

    private final String name;
    private File file;
    private YamlConfiguration config;

    public RecipeConfig(JavaPlugin plugin, File file) {
        this.name = file.getName().substring(0, 4);
        this.file = file;
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}