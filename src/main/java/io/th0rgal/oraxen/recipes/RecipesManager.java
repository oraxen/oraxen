package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.recipes.listeners.RecipesBuilderEvents;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.recipes.loaders.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

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
        reload(Bukkit.getConsoleSender());
    }

    public static boolean reload(@Nullable CommandSender sender) {
        // Folia's recipe updates can reload player advancements off their owning regions.
        if (VersionUtil.isFoliaServer()) {
            Message.RECIPE_RELOAD_REQUIRES_RESTART.send(sender != null ? sender : Bukkit.getConsoleSender());
            return false;
        }
        if (!SchedulerUtil.isGlobalThread()) {
            SchedulerUtil.runTask(RecipesManager::reload);
            return true;
        }
        RecipesEventsManager events = RecipesEventsManager.get();
        if (loadRecipes()) for (Player player : Bukkit.getOnlinePlayers())
            SchedulerUtil.runForEntity(player, () -> events.discoverRecipes(player));
        events.registerEvents();
        return true;
    }

    private static boolean loadRecipes() {
        File folder = new File(OraxenPlugin.get().getDataFolder(), "recipes");
        if (!folder.exists() && folder.mkdirs() && Settings.GENERATE_DEFAULT_CONFIGS.toBool())
            OraxenPlugin.get().getResourceManager().extractConfigsInFolder("recipes", "yml");
        createRecipeFiles(folder);
        return registerAllConfigRecipesFromFolder(folder);
    }

    private static boolean registerAllConfigRecipesFromFolder(File folder) {
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
                if (config.getKeys(false).isEmpty()) continue;
                Function<ConfigurationSection, RecipeLoader> loader = loader(file.getName());
                for (String name : config.getKeys(false)) {
                    RecipeLoader recipe = null;
                    try {
                        ConfigurationSection section = config.getConfigurationSection(name);
                        recipe = loader.apply(section != null ? section : new MemoryConfiguration().createSection(name));
                        if (section == null) throw new IllegalArgumentException("Entry must be a recipe section");
                        recipe.registerRecipe();
                    } catch (RuntimeException exception) {
                        Logs.logError("Could not load recipe " + file.getName() + "." + name + ": " + exception.getMessage());
                        Logs.debug(exception);
                        if (recipe != null) recipe.retainPreviousVersion();
                    }
                }
            }
            YamlConfiguration disabled = read(new File(folder, "disabled.yml"));
            for (String id : disabled.getStringList("disabled"))
                if (NamespacedKey.fromString(id) == null) throw new IllegalArgumentException("Invalid disabled recipe " + id);
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
        return completed;
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

    private static Function<ConfigurationSection, RecipeLoader> loader(String file) {
        return switch (file) {
            case "shaped.yml", "shaped-transform.yml" -> ShapedLoader::new;
            case "shapeless.yml", "shapeless-transform.yml" -> ShapelessLoader::new;
            case "furnace.yml" -> FurnaceLoader::new;
            case "blasting.yml" -> BlastingLoader::new;
            case "campfire.yml" -> CampfireLoader::new;
            case "smoking.yml" -> SmokingLoader::new;
            case "stonecutting.yml" -> StonecuttingLoader::new;
            case "smithing.yml" -> SmithingLoader::new;
            case "brewing.yml" -> BrewingLoader::new;
            case "cauldron.yml" -> section -> new WorkstationLoader(section, CustomWorkstationRecipe.Type.CAULDRON);
            case "anvil.yml" -> section -> new WorkstationLoader(section, CustomWorkstationRecipe.Type.ANVIL);
            case "grindstone.yml" -> section -> new WorkstationLoader(section, CustomWorkstationRecipe.Type.GRINDSTONE);
            default -> throw new IllegalArgumentException("Unknown recipe file " + file);
        };
    }
}
