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
    private static final Map<NamespacedKey, RecipeBrewing> registered = new LinkedHashMap<>();
    private static Map<NamespacedKey, RecipeBrewing> staging;

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

    public static synchronized void beginReload() {
        if (staging != null) throw new IllegalStateException("Brewing reload already in progress");
        staging = new LinkedHashMap<>();
    }

    public static synchronized void register(RecipeBrewing recipe) {
        if (staging == null) throw new IllegalStateException("Brewing reload has not begun");
        if (staging.containsKey(recipe.key()))
            throw new IllegalArgumentException("Duplicate brewing recipe " + recipe.key());
        prepare(recipe);
        staging.put(recipe.key(), recipe);
    }

    public static synchronized void cancelReload() { staging = null; }

    public static synchronized void validateReload() {
        if (staging == null) throw new IllegalStateException("Brewing reload has not begun");
        staging.values().forEach(BrewingLoader::prepare);
    }

    public static synchronized void finishReload() {
        if (staging == null) throw new IllegalStateException("Brewing reload has not begun");
        Map<NamespacedKey, Object> prepared = new LinkedHashMap<>();
        // Resolve compatibility before removing any live recipes.
        staging.forEach((key, recipe) -> prepared.put(key, prepare(recipe)));
        Map<NamespacedKey, RecipeBrewing> previous = new LinkedHashMap<>(registered);
        clearRegistered();
        try {
            for (Map.Entry<NamespacedKey, RecipeBrewing> entry : staging.entrySet()) {
                add(prepared.get(entry.getKey()));
                registered.put(entry.getKey(), entry.getValue());
            }
            staging = null;
        } catch (RuntimeException failure) {
            clearRegistered();
            previous.forEach((key, recipe) -> {
                try {
                    add(prepare(recipe));
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
        if (!registered.containsKey(recipe.key()) && Bukkit.getRecipe(recipe.key()) != null)
            throw new IllegalArgumentException("Brewing recipe key already belongs to another recipe " + recipe.key());
        try {
            return constructor.newInstance(recipe.key(), recipe.result(), container, ingredient);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create native brewing recipe " + recipe.key(), cause(e));
        }
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
