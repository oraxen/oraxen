package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.recipes.listeners.RecipesBuilderEvents;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.recipes.loaders.*;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.SchedulerUtil;
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
import java.util.List;
import java.util.Objects;

public class RecipesManager {

    private RecipesManager() {}
    private static boolean builderEventsRegistered;

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

        if (!builderEventsRegistered) {
            Bukkit.getPluginManager().registerEvents(new RecipesBuilderEvents(), plugin);
            builderEventsRegistered = true;
        }
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                OraxenPlugin.get().getResourceManager().extractConfigsInFolder("recipes", "yml");
        }
        createRecipeFiles(recipesFolder);
        registerAllConfigRecipesFromFolder(recipesFolder);
        RecipesEventsManager.get().registerEvents();
    }

    public static void reload() {
        // The server recipe registry is global state mutated in place; a reload
        // issued from a player's region thread would race concurrent crafting
        // resolution on other regions. Re-dispatch onto the global region
        // thread, mirroring the jukebox/painting registry reloads.
        if (!SchedulerUtil.isGlobalThread()) {
            SchedulerUtil.runTask(RecipesManager::reload);
            return;
        }
        if (Settings.RESET_RECIPES.toBool()) {
            Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
            while (recipeIterator.hasNext()) {
                NamespacedKey recipeID = ((Keyed) recipeIterator.next()).getKey();
                if (recipeID.getNamespace().equals("oraxen")) {
                    Bukkit.removeRecipe(recipeID);
                }
            }
        }

        RecipesEventsManager.get().resetRecipes();
        File recipesFolder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                OraxenPlugin.get().getResourceManager().extractConfigsInFolder("recipes", "yml");
        }
        createRecipeFiles(recipesFolder);
        registerAllConfigRecipesFromFolder(recipesFolder);
        RecipesEventsManager.get().registerEvents();
    }

    private static void registerAllConfigRecipesFromFolder(File recipesFolder) {
        RecipeLoader.beginRegistrationPass();
        CustomWorkstationRegistry.beginReload();
        RecipesEventsManager.get().beginSmithingReload();
        try {
            for (File configFile : Objects.requireNonNull(recipesFolder.listFiles()))
                registerConfigRecipes(configFile);
        } finally {
            CustomWorkstationRegistry.finishReload();
            RecipesEventsManager.get().finishSmithingReload();
        }
    }

    /**
     * Creates any missing default recipe files as empty templates. Runs on every load so servers
     * upgrading to a release that adds new recipe types (smithing/cauldron/anvil/grindstone)
     * receive the files too; previously they were only written when the whole folder was missing,
     * and the backfill was inverted to run only with default generation disabled. Bundled example
     * recipes are deliberately only extracted into a freshly created folder, never resurrected
     * into existing setups.
     */
    private static void createRecipeFiles(File recipesFolder) {
        for (String name : List.of("furnace.yml", "shaped.yml", "shapeless.yml", "blasting.yml", "campfire.yml",
                "smoking.yml", "stonecutting.yml", "smithing.yml", "cauldron.yml", "anvil.yml", "grindstone.yml", "disabled.yml")) {
            try {
                new File(recipesFolder, name).createNewFile();
            } catch (IOException exception) {
                Logs.logError("Error while creating recipe file " + name + ": " + exception.getMessage());
            }
        }
    }

    private static void registerConfigRecipes(File configFile) {
        YamlConfiguration config = OraxenYaml.loadConfiguration(configFile);
        if (configFile.getName().equals("disabled.yml")) {
            disableRecipes(config);
            return;
        }
        for (String recipeSetting : config.getKeys(false)) {
            if (!config.isConfigurationSection(recipeSetting))
                continue;
            ConfigurationSection recipeSection = config.getConfigurationSection(recipeSetting);
            registerRecipeByType(configFile, recipeSection);
        }
    }

    private static void disableRecipes(YamlConfiguration config) {
        List<String> disabledRecipes = config.getStringList("disabled");
        for (String recipeName : disabledRecipes) {
            NamespacedKey key = NamespacedKey.fromString(recipeName);
            if (key != null) {
                if (Bukkit.removeRecipe(key)) {
                    if (Settings.DEBUG.toBool())
                        Logs.logInfo("Successfully disabled recipe " + key);
                } else
                    Logs.logWarning("Could not disable recipe (not found): " + key);
            } else {
                Logs.logWarning("Invalid recipe key in disabled.yml: " + recipeName);
            }
        }
    }

    private static void registerRecipeByType(File configFile, ConfigurationSection recipeSection) {
        try {
            switch (configFile.getName()) {
                case "shaped.yml" -> new ShapedLoader(recipeSection).registerRecipe();
                case "shapeless.yml" -> new ShapelessLoader(recipeSection).registerRecipe();
                case "furnace.yml" -> new FurnaceLoader(recipeSection).registerRecipe();
                case "blasting.yml" -> new BlastingLoader(recipeSection).registerRecipe();
                case "campfire.yml" -> new CampfireLoader(recipeSection).registerRecipe();
                case "smoking.yml" -> new SmokingLoader(recipeSection).registerRecipe();
                case "stonecutting.yml" -> new StonecuttingLoader(recipeSection).registerRecipe();
                case "smithing.yml" -> new SmithingLoader(recipeSection).registerRecipe();
                case "cauldron.yml" -> new WorkstationLoader(recipeSection, CustomWorkstationRecipe.Type.CAULDRON).registerRecipe();
                case "anvil.yml" -> new WorkstationLoader(recipeSection, CustomWorkstationRecipe.Type.ANVIL).registerRecipe();
                case "grindstone.yml" -> new WorkstationLoader(recipeSection, CustomWorkstationRecipe.Type.GRINDSTONE).registerRecipe();
                default -> Logs.logError(configFile.getName());
            }
        } catch (Exception exception) {
            Message.BAD_RECIPE.log(AdventureUtils.tagResolver("recipe", recipeSection.getName()));
            Logs.logError("Failed to load recipe '" + recipeSection.getName() + "': " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            Logs.debug(exception);
        }
    }
}
