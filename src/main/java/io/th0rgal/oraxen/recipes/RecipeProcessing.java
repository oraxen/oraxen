package io.th0rgal.oraxen.recipes;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.block.CampfireStartEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecutterInventory;
import org.bukkit.inventory.StonecuttingRecipe;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecipeProcessing implements Listener {
    private static volatile Map<NamespacedKey, RecipeChoice> choices = Map.of();
    private static Map<NamespacedKey, RecipeChoice> staging;

    public static void beginReload() { staging = new LinkedHashMap<>(); }

    public static void register(Recipe recipe, RecipeChoice choice) {
        if (staging == null) throw new IllegalStateException("Recipe reload has not begun");
        staging.put(((Keyed) recipe).getKey(), choice.clone());
    }

    public static void finishReload() {
        choices = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(staging));
        staging = null;
    }

    public static void cancelReload() { staging = null; }

    public static boolean matches(Recipe recipe, ItemStack input) {
        if (!(recipe instanceof Keyed keyed)) return true;
        RecipeChoice choice = choices.get(keyed.getKey());
        return choice == null || input != null && choice.test(input);
    }

    public static Recipe resolve(Recipe selected, ItemStack input) {
        if (matches(selected, input)) return selected;
        for (Map.Entry<NamespacedKey, RecipeChoice> entry : choices.entrySet()) {
            if (!entry.getValue().test(input)) continue;
            Recipe candidate = org.bukkit.Bukkit.getRecipe(entry.getKey());
            if (candidate != null && candidate.getClass() == selected.getClass()) return candidate;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStartSmelt(FurnaceStartSmeltEvent event) {
        Recipe candidate = resolve(event.getRecipe(), event.getSource());
        if (candidate instanceof CookingRecipe<?> cooking) event.setTotalCookTime(cooking.getCookingTime());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStartCampfire(CampfireStartEvent event) {
        Recipe candidate = resolve(event.getRecipe(), event.getSource());
        if (candidate instanceof CookingRecipe<?> cooking) event.setTotalCookTime(cooking.getCookingTime());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCook(BlockCookEvent event) {
        Recipe recipe = cookingRecipe(event);
        if (recipe != null) {
            Recipe candidate = resolve(recipe, event.getSource());
            if (candidate == null) event.setCancelled(true);
            else if (candidate != recipe) {
                event.setResult(candidate.getResult().clone());
                correctCookingExperience(event, recipe, candidate);
            }
            return;
        }
        // Older APIs do not expose the selected recipe on this event.
        for (Map.Entry<NamespacedKey, RecipeChoice> entry : choices.entrySet()) {
            Recipe candidate = org.bukkit.Bukkit.getRecipe(entry.getKey());
            if (!(candidate instanceof CookingRecipe<?> cooking)) continue;
            if (!cooking.getResult().isSimilar(event.getResult())) continue;
            if (cooking.getInputChoice().test(event.getSource()) && !entry.getValue().test(event.getSource())) {
                Recipe replacement = resolve(candidate, event.getSource());
                if (replacement == null) event.setCancelled(true);
                else {
                    event.setResult(replacement.getResult().clone());
                    correctCookingExperience(event, candidate, replacement);
                }
                return;
            }
        }
    }

    private static void correctCookingExperience(BlockCookEvent event, Recipe selected, Recipe matched) {
        if (!(selected instanceof CookingRecipe<?> original) || !(matched instanceof CookingRecipe<?> replacement)
                || !(event.getBlock().getState() instanceof org.bukkit.block.Furnace)) return;
        io.th0rgal.oraxen.utils.SchedulerUtil.runAtLocation(event.getBlock().getLocation(), () -> {
            if (event.isCancelled() || !(event.getBlock().getState() instanceof org.bukkit.block.Furnace furnace)) return;
            Map<CookingRecipe<?>, Integer> used = new LinkedHashMap<>(furnace.getRecipesUsed());
            int count = used.getOrDefault(original, 0);
            if (count <= 0) return;
            if (count == 1) used.remove(original);
            else used.put(original, count - 1);
            used.merge(replacement, 1, Integer::sum);
            furnace.setRecipesUsed(used);
            furnace.update();
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStonecutting(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof StonecutterInventory inventory) || event.getRawSlot() != 1) return;
        StonecuttingRecipe recipe = selectedStonecuttingRecipe(event, inventory);
        if (!matches(recipe, inventory.getInputItem())) {
            event.setCancelled(true);
            inventory.setResult(null);
        }
    }

    private static StonecuttingRecipe selectedStonecuttingRecipe(InventoryClickEvent event, StonecutterInventory inventory) {
        for (Object target : new Object[]{event.getView(), inventory}) {
            try {
                int index = (Integer) target.getClass().getMethod("getSelectedRecipeIndex").invoke(target);
                java.util.List<?> recipes = (java.util.List<?>) target.getClass().getMethod("getRecipes").invoke(target);
                return index >= 0 && index < recipes.size() ? (StonecuttingRecipe) recipes.get(index) : null;
            } catch (ReflectiveOperationException ignored) {
                // Older servers expose selection on the inventory rather than the view.
            }
        }
        throw new IllegalStateException("Cannot validate stonecutting selection on this server");
    }

    private static Recipe cookingRecipe(BlockCookEvent event) {
        try {
            Method method = event.getClass().getMethod("getRecipe");
            return (Recipe) method.invoke(event);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
