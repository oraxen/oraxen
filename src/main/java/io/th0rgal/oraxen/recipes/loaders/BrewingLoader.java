package io.th0rgal.oraxen.recipes.loaders;

import io.papermc.paper.potion.PotionMix;
import io.th0rgal.oraxen.recipes.RecipeBrewing;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class BrewingLoader extends RecipeLoader {
    // Values are the prepared PotionMix or native brewing recipe.
    private static final Map<NamespacedKey, Object> registered = new LinkedHashMap<>();
    private static Map<NamespacedKey, Object> staging;

    public BrewingLoader(ConfigurationSection section) { super(section); }

    @Override
    public void registerRecipe() {
        RecipeChoice container = getRecipeChoice(getSection().get("container"));
        RecipeChoice ingredient = getRecipeChoice(getSection().get("ingredient"));
        if (container == null || ingredient == null)
            throw new IllegalArgumentException("Brewing recipe requires container and ingredient");
        // A brewing cycle consumes one reagent, regardless of the number of matching bottles.
        if (io.th0rgal.oraxen.recipes.RecipeIngredients.amount(getSection().get("container")) != 1
                || io.th0rgal.oraxen.recipes.RecipeIngredients.amount(getSection().get("ingredient")) != 1)
            throw new IllegalArgumentException("Brewing container and ingredient amounts must be one");
        register(new RecipeBrewing(getNamespacedKey(), container, ingredient, getValidResult()));
    }

    @Override
    public void retainPreviousVersion() {
        if (resetsRecipes()) return;
        try {
            retain(getNamespacedKey());
        } catch (IllegalArgumentException ignored) {
            // An invalid name never had a working version.
        }
    }

    private static synchronized void retain(NamespacedKey key) {
        if (staging == null) throw new IllegalStateException("Brewing reload has not begun");
        if (registered.containsKey(key)) staging.putIfAbsent(key, registered.get(key));
    }

    public static synchronized void beginReload() {
        if (staging != null) throw new IllegalStateException("Brewing reload already in progress");
        staging = new LinkedHashMap<>();
    }

    public static synchronized void register(RecipeBrewing recipe) {
        if (staging == null) throw new IllegalStateException("Brewing reload has not begun");
        if (staging.containsKey(recipe.key()))
            throw new IllegalArgumentException("Duplicate brewing recipe " + recipe.key());
        staging.put(recipe.key(), prepare(recipe));
    }

    public static synchronized void cancelReload() { staging = null; }

    public static synchronized void finishReload() {
        if (staging == null) throw new IllegalStateException("Brewing reload has not begun");
        // Crafting recipes registered earlier in this reload can take a native brewing key.
        staging.forEach((key, recipe) -> {
            if (!(recipe instanceof PotionMix)) requireOwnedKey(key);
        });
        Map<NamespacedKey, Object> previous = new LinkedHashMap<>(registered);
        clearRegistered();
        try {
            for (Map.Entry<NamespacedKey, Object> entry : staging.entrySet()) {
                add(entry.getValue());
                registered.put(entry.getKey(), entry.getValue());
            }
            staging = null;
        } catch (RuntimeException failure) {
            clearRegistered();
            previous.forEach((key, recipe) -> {
                try {
                    add(recipe);
                    registered.put(key, recipe);
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            });
            staging = null;
            throw failure;
        }
    }

    public static synchronized void clear() {
        staging = null;
        clearRegistered();
    }

    private static void clearRegistered() {
        for (NamespacedKey key : registered.keySet()) {
            if (nativeConstructor() == null) Bukkit.getPotionBrewer().removePotionMix(key);
            else Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    private static Object prepare(RecipeBrewing recipe) {
        RecipeChoice container = brewingChoice(recipe.container());
        RecipeChoice ingredient = brewingChoice(recipe.ingredient());
        Constructor<?> constructor = nativeConstructor();
        if (constructor == null) return new PotionMix(recipe.key(), recipe.result(), container, ingredient);
        requireOwnedKey(recipe.key());
        try {
            return constructor.newInstance(recipe.key(), recipe.result(), container, ingredient);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create native brewing recipe " + recipe.key(), cause(e));
        }
    }

    private static void requireOwnedKey(NamespacedKey key) {
        if (!registered.containsKey(key) && Bukkit.getRecipe(key) != null)
            throw new IllegalArgumentException("Brewing recipe key already belongs to another recipe " + key);
    }

    private static Constructor<?> nativeConstructor() {
        try {
            return Class.forName("org.bukkit.inventory.BrewingRecipe").getConstructor(
                    NamespacedKey.class, ItemStack.class, RecipeChoice.class, RecipeChoice.class);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported native brewing API", e);
        }
    }

    private static RecipeChoice brewingChoice(RecipeChoice choice) {
        try {
            Method factory = PotionMix.class.getMethod("createPredicateChoice", Predicate.class);
            return (RecipeChoice) factory.invoke(null, (Predicate<ItemStack>) choice::test);
        } catch (NoSuchMethodException e) {
            // Early Paper versions only convert concrete exact and material choices.
            if (choice.getClass() == RecipeChoice.ExactChoice.class
                    || choice.getClass() == RecipeChoice.MaterialChoice.class) return choice;
            throw new IllegalArgumentException("This Paper version does not support brewing ingredient predicates", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create brewing predicate", cause(e));
        }
    }

    private static void add(Object recipe) {
        // Let the server own timing, fuel, bottle matching and reagent consumption.
        if (recipe instanceof PotionMix mix) Bukkit.getPotionBrewer().addPotionMix(mix);
        else if (!Bukkit.addRecipe((Recipe) recipe))
            throw new IllegalStateException("Server rejected native brewing recipe");
    }

    private static Throwable cause(ReflectiveOperationException e) {
        return e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
    }
}
