package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.RecipeIngredients;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class RecipeLoader {

    private static final java.util.Set<NamespacedKey> KEYS_REGISTERED_THIS_PASS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final Map<NamespacedKey, Recipe> pendingRecipes = new LinkedHashMap<>();
    private static final Map<NamespacedKey, Map<Integer, List<ItemStack>>> pendingDisplays = new LinkedHashMap<>();
    private static volatile Map<NamespacedKey, Map<Integer, List<ItemStack>>> ingredientDisplays = Map.of();
    private static Map<NamespacedKey, Map<Integer, List<ItemStack>>> previousDisplays = Map.of();
    private static Set<NamespacedKey> activeKeys = Set.of();
    private static boolean reset;
    private static boolean staging;
    private static boolean applied;
    private static Map<NamespacedKey, Recipe> previousRecipes = Map.of();
    private static Set<NamespacedKey> previousKeys = Set.of();
    private static Map<String, RecipeChoice> previousTags = Map.of();

    private final ConfigurationSection section;

    protected RecipeLoader(ConfigurationSection section) {
        this.section = section;
    }

    /** Called before each full recipe registration pass so duplicate keys within the pass can be detected. */
    public static void beginRegistrationPass() {
        beginRegistrationPass(true);
    }

    public static void beginRegistrationPass(boolean resetRecipes) {
        if (staging) throw new IllegalStateException("Recipe registration pass already active");
        reset = resetRecipes;
        KEYS_REGISTERED_THIS_PASS.clear();
        pendingRecipes.clear();
        previousTags = RecipeIngredients.tags();
        previousDisplays = ingredientDisplays;
        previousKeys = activeKeys;
        applied = false;
        staging = true;
    }

    public static void finishRegistrationPass() {
        if (!staging || applied) throw new IllegalStateException("Recipe registration pass has not begun or was already applied");
        Set<NamespacedKey> touched = new LinkedHashSet<>(reset ? activeKeys : Set.of());
        touched.addAll(pendingRecipes.keySet());
        Map<NamespacedKey, Recipe> previous = new LinkedHashMap<>();
        for (NamespacedKey key : touched) {
            Recipe recipe = Bukkit.getRecipe(key);
            if (recipe != null) previous.put(key, recipe);
        }
        Map<NamespacedKey, Map<Integer, List<ItemStack>>> displays = new LinkedHashMap<>();
        for (NamespacedKey key : retainedKeys()) {
            if (previousDisplays.containsKey(key)) displays.put(key, previousDisplays.get(key));
        }
        displays.putAll(pendingDisplays);
        ingredientDisplays = Map.copyOf(displays);
        try {
            touched.forEach(Bukkit::removeRecipe);
            pendingRecipes.forEach((key, recipe) -> {
                if (!Bukkit.addRecipe(recipe)) throw new IllegalStateException("Bukkit rejected recipe '" + key + "'");
            });
        } catch (RuntimeException exception) {
            ingredientDisplays = previousDisplays;
            touched.forEach(Bukkit::removeRecipe);
            previous.forEach((key, recipe) -> {
                if (!Bukkit.addRecipe(recipe))
                    exception.addSuppressed(new IllegalStateException("Could not restore recipe '" + key + "'"));
            });
            throw exception;
        }
        for (NamespacedKey key : retainedKeys()) {
            Recipe recipe = Bukkit.getRecipe(key);
            if (recipe != null) previous.put(key, recipe);
        }
        previousRecipes = previous;
        Set<NamespacedKey> nextKeys = new LinkedHashSet<>(retainedKeys());
        nextKeys.addAll(pendingRecipes.keySet());
        activeKeys = Set.copyOf(nextKeys);
        applied = true;
    }

    public static Set<NamespacedKey> retainedKeys() {
        if (reset) return Set.of();
        Set<NamespacedKey> retained = new LinkedHashSet<>(previousKeys);
        retained.removeAll(pendingRecipes.keySet());
        return Set.copyOf(retained);
    }

    public static void completeRegistrationPass() {
        if (!staging || !applied) throw new IllegalStateException("Recipe registration has not been applied");
        clearRegistrationPass();
    }

    public static void cancelRegistrationPass() {
        if (!staging) return;
        RuntimeException failure = null;
        if (applied) {
            ingredientDisplays = previousDisplays;
            Set<NamespacedKey> touched = new LinkedHashSet<>(activeKeys);
            touched.addAll(previousKeys);
            touched.forEach(Bukkit::removeRecipe);
            for (Map.Entry<NamespacedKey, Recipe> entry : previousRecipes.entrySet()) {
                if (!Bukkit.addRecipe(entry.getValue())) {
                    if (failure == null) failure = new IllegalStateException("Could not restore recipes after cancelled reload");
                    failure.addSuppressed(new IllegalStateException("Could not restore recipe '" + entry.getKey() + "'"));
                }
            }
            activeKeys = previousKeys;
        }
        RecipeIngredients.publishTags(previousTags);
        clearRegistrationPass();
        if (failure != null) throw failure;
    }

    private static void clearRegistrationPass() {
        pendingRecipes.clear();
        previousRecipes = Map.of();
        pendingDisplays.clear();
        previousDisplays = Map.of();
        previousKeys = Set.of();
        previousTags = Map.of();
        KEYS_REGISTERED_THIS_PASS.clear();
        applied = false;
        staging = false;
    }

    protected ConfigurationSection getSection() {
        return section;
    }

    protected ItemStack getResult() {
        return RecipeIngredients.result(section.get("result"));
    }

    protected ItemStack getValidResult() {
        ItemStack result = getResult();
        if (result == null || result.getType().isAir())
            throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an invalid or missing result");
        return result;
    }

    protected String getRecipeName() {
        return section.getName();
    }

    protected ItemStack getIndredientItemStack(ConfigurationSection ingredientSection) {
        return RecipeIngredients.result(ingredientSection);
    }

    protected RecipeChoice getRecipeChoice(Object ingredient) {
        return RecipeIngredients.choice(ingredient);
    }

    protected RecipeChoice getWorkbenchRecipeChoice(Object ingredient) {
        return RecipeIngredients.choice(ingredient, true);
    }

    protected NamespacedKey getNamespacedKey() {
        return new NamespacedKey(OraxenPlugin.get(), getRecipeName());
    }

    protected String getGroup() {
        return getSection().getString("group", "");
    }

    public abstract void registerRecipe();

    protected void loadRecipe(Recipe recipe) {
        Map<Integer, List<ItemStack>> display = RecipeIngredients.displayOverrides(recipe);
        Recipe registration = serverRecipe(recipe);
        if (!(registration instanceof Keyed keyed))
            throw new IllegalArgumentException("Recipe must have a namespaced key");
        if (!KEYS_REGISTERED_THIS_PASS.add(keyed.getKey()))
            throw new IllegalArgumentException("Duplicate recipe key '" + keyed.getKey() + "'");
        if (staging) {
            pendingRecipes.put(keyed.getKey(), registration);
            if (!display.isEmpty()) pendingDisplays.put(keyed.getKey(), display);
        } else {
            Bukkit.removeRecipe(keyed.getKey());
            if (!Bukkit.addRecipe(registration))
                throw new IllegalStateException("Bukkit rejected recipe '" + getRecipeName() + "'");
            Map<NamespacedKey, Map<Integer, List<ItemStack>>> displays = new LinkedHashMap<>(ingredientDisplays);
            if (display.isEmpty()) displays.remove(keyed.getKey());
            else displays.put(keyed.getKey(), display);
            ingredientDisplays = Map.copyOf(displays);
        }
        managesPermission(CustomRecipe.fromRecipe(recipe));
    }

    public static Map<Integer, List<ItemStack>> ingredientDisplays(NamespacedKey key) {
        return ingredientDisplays.getOrDefault(key, Map.of());
    }

    private static Recipe serverRecipe(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            shaped.getChoiceMap().forEach((symbol, choice) -> {
                if (choice != null) shaped.setIngredient(symbol, RecipeIngredients.serverChoice(choice));
            });
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            ShapelessRecipe copy = new ShapelessRecipe(shapeless.getKey(), shapeless.getResult());
            copy.setGroup(shapeless.getGroup());
            copy.setCategory(shapeless.getCategory());
            shapeless.getChoiceList().forEach(choice -> copy.addIngredient(RecipeIngredients.serverChoice(choice)));
            return copy;
        } else if (recipe instanceof CookingRecipe<?> cooking) {
            cooking.setInputChoice(RecipeIngredients.cookingChoice(cooking.getInputChoice()));
        } else if (recipe instanceof StonecuttingRecipe stonecutting) {
            stonecutting.setInputChoice(RecipeIngredients.serverChoice(stonecutting.getInputChoice()));
        } else if (recipe instanceof SmithingTransformRecipe smithing) {
            return new SmithingTransformRecipe(smithing.getKey(), smithing.getResult(),
                    RecipeIngredients.serverChoice(smithing.getTemplate()), RecipeIngredients.serverChoice(smithing.getBase()),
                    RecipeIngredients.serverChoice(smithing.getAddition()));
        } else if (recipe instanceof SmithingRecipe smithing) {
            return new SmithingRecipe(smithing.getKey(), smithing.getResult(),
                    RecipeIngredients.serverChoice(smithing.getBase()), RecipeIngredients.serverChoice(smithing.getAddition()));
        }
        return recipe;
    }

    private void managesPermission(CustomRecipe recipe) {
        if (recipe != null && getSection().isString("permission")) {
            String permission = getSection().getString("permission");
            RecipesEventsManager.get().addPermissionRecipe(recipe, permission);
        }
    }

    protected String getPermission() {
        return getSection().getString("permission");
    }

    protected RecipeChoice getRequiredChoice(String key) {
        RecipeChoice choice = getRecipeChoice(getSection().get(key));
        if (choice == null) throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an invalid " + key + " ingredient");
        return choice;
    }

    protected int getIngredientAmount(String key) {
        return RecipeIngredients.amount(getSection().get(key));
    }

    protected void addToWhitelistedRecipes(Recipe recipe) {
        RecipesEventsManager.get().whitelistRecipe(CustomRecipe.fromRecipe(recipe));
    }

}
