package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.recipes.listeners.RecipesBuilderEvents;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.recipes.loaders.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RecipesManager {
    private static boolean builderEventsRegistered;

    public static void load(OraxenPlugin plugin) {
        if (!builderEventsRegistered) {
            Bukkit.getPluginManager().registerEvents(new RecipesBuilderEvents(), plugin);
            Bukkit.getPluginManager().registerEvents(new RecipeProcessing(), plugin);
            builderEventsRegistered = true;
        }
        loadRecipes();
        RecipesEventsManager.get().registerEvents();
    }

    public static void reload() {
        if (!SchedulerUtil.isGlobalThread()) {
            SchedulerUtil.runTask(RecipesManager::reload);
            return;
        }
        loadRecipes();
        RecipesEventsManager.get().registerEvents();
    }

    private static void loadRecipes() {
        File folder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!folder.exists() && folder.mkdirs() && Settings.GENERATE_DEFAULT_CONFIGS.toBool())
            OraxenPlugin.get().getResourceManager().extractConfigsInFolder("recipes", "yml");
        createRecipeFiles(folder);
        registerAllConfigRecipesFromFolder(folder);
    }

    private static void registerAllConfigRecipesFromFolder(File folder) {
        RecipeLoader.beginRegistrationPass(Settings.RESET_RECIPES.toBool());
        RecipeBrewing.beginReload();
        RecipeProcessing.beginReload();
        CustomWorkstationRegistry.beginReload();
        RecipesEventsManager events = RecipesEventsManager.get();
        events.resetRecipes();
        events.beginSmithingReload();
        boolean completed = false;
        try {
            File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".yml"));
            if (files == null) throw new IOException("Could not list recipe files");
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File file : files) {
                if (!file.getName().equals("tags.yml") && !file.getName().equals("disabled.yml"))
                    RecipeMigration.migrate(file);
            }
            RecipeIngredients.loadTags(read(new File(folder, "tags.yml")));
            for (File file : files) {
                if (file.getName().equals("tags.yml") || file.getName().equals("disabled.yml")) continue;
                YamlConfiguration config = read(file);
                for (String name : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(name);
                    if (section == null) throw new IllegalArgumentException(file.getName() + "." + name + " must be a recipe section");
                    try {
                        registerRecipeByType(file.getName(), section);
                    } catch (RuntimeException exception) {
                        throw new IllegalArgumentException("Invalid recipe " + file.getName() + "." + name + ": " + exception.getMessage(), exception);
                    }
                }
            }
            YamlConfiguration disabled = read(new File(folder, "disabled.yml"));
            for (String id : disabled.getStringList("disabled"))
                if (NamespacedKey.fromString(id) == null) throw new IllegalArgumentException("Invalid disabled recipe " + id);
            RecipeBrewing.validateReload();
            events.retainRecipes(RecipeLoader.retainedKeys());
            RecipeProcessing.retainRecipes(RecipeLoader.retainedKeys());
            RecipeLoader.finishRegistrationPass();
            RecipeBrewing.finishReload();
            RecipeProcessing.finishReload();
            CustomWorkstationRegistry.finishReload();
            events.finishSmithingReload();
            events.finishRecipeReload();
            disableRecipes(disabled);
            RecipeLoader.completeRegistrationPass();
            completed = true;
        } catch (Exception exception) {
            Logs.logError("Could not reload recipes. Previous recipes have been retained: " + exception.getMessage());
            Logs.debug(exception);
        } finally {
            if (!completed) {
                RecipeLoader.cancelRegistrationPass();
                RecipeBrewing.cancelReload();
                RecipeProcessing.cancelReload();
                CustomWorkstationRegistry.cancelReload();
                events.cancelSmithingReload();
                events.cancelRecipeReload();
            }
        }
    }

    private static YamlConfiguration read(File file) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        return config;
    }

    private static void createRecipeFiles(File folder) {
        for (String name : List.of("furnace", "shaped", "shapeless", "blasting", "campfire", "smoking", "stonecutting",
                "smithing", "cauldron", "anvil", "grindstone", "brewing", "shaped-transform", "shapeless-transform", "tags", "disabled")) {
            try {
                new File(folder, name + ".yml").createNewFile();
            } catch (IOException exception) {
                Logs.logError("Could not create recipe file " + name + ".yml");
                Logs.debug(exception);
            }
        }
    }

    private static void disableRecipes(YamlConfiguration config) {
        for (String name : config.getStringList("disabled")) {
            NamespacedKey key = NamespacedKey.fromString(name);
            if (key != null) Bukkit.removeRecipe(key);
        }
    }

    private static void registerRecipeByType(String file, ConfigurationSection section) {
        switch (file) {
            case "shaped.yml", "shaped-transform.yml" -> new ShapedLoader(section).registerRecipe();
            case "shapeless.yml", "shapeless-transform.yml" -> new ShapelessLoader(section).registerRecipe();
            case "furnace.yml" -> new FurnaceLoader(section).registerRecipe();
            case "blasting.yml" -> new BlastingLoader(section).registerRecipe();
            case "campfire.yml" -> new CampfireLoader(section).registerRecipe();
            case "smoking.yml" -> new SmokingLoader(section).registerRecipe();
            case "stonecutting.yml" -> new StonecuttingLoader(section).registerRecipe();
            case "smithing.yml" -> new SmithingLoader(section).registerRecipe();
            case "brewing.yml" -> new BrewingLoader(section).registerRecipe();
            case "cauldron.yml" -> new WorkstationLoader(section, CustomWorkstationRecipe.Type.CAULDRON).registerRecipe();
            case "anvil.yml" -> new WorkstationLoader(section, CustomWorkstationRecipe.Type.ANVIL).registerRecipe();
            case "grindstone.yml" -> new WorkstationLoader(section, CustomWorkstationRecipe.Type.GRINDSTONE).registerRecipe();
            default -> throw new IllegalArgumentException("Unknown recipe file " + file);
        }
    }
}
