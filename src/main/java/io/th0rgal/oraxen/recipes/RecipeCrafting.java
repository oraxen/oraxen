package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.*;

/** Immutable matchers shared by crafting tables and automated crafters. */
public final class RecipeCrafting {
    private RecipeCrafting() {}

    public static void registerShaped(ShapedRecipe recipe, ConfigurationSection section,
                                      Map<Character, RecipeChoice> choices) {
        RecipeTransforms.Transform transform = RecipeTransforms.parse(section);
        Character source = null;
        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients != null) for (String symbol : ingredients.getKeys(false)) {
            if (!isSource(ingredients.get(symbol))) continue;
            if (source != null) throw new IllegalArgumentException("Multiple transform sources");
            source = symbol.charAt(0);
        }
        if (section.isString("transform.source")) {
            String configured = section.getString("transform.source");
            if (configured.length() != 1 || source != null)
                throw new IllegalArgumentException("Invalid or duplicate transform source");
            source = configured.charAt(0);
        }
        RecipesEventsManager.get().registerShapedRecipe(recipe.getKey(), List.of(recipe.getShape()), choices,
                source, transform);
    }

    public static void registerShapeless(ShapelessRecipe recipe, ConfigurationSection section,
                                         List<RecipeChoice> choices) {
        RecipeTransforms.Transform transform = RecipeTransforms.parse(section);
        int source = -1;
        int index = 0;
        Object raw = section.get("ingredients");
        Collection<?> ingredients = raw instanceof ConfigurationSection legacy
                ? legacy.getValues(false).values() : raw instanceof List<?> list ? list : List.of();
        for (Object ingredient : ingredients) {
            int amount = RecipeIngredients.amount(ingredient);
            if (isSource(ingredient)) {
                if (source != -1 || amount != 1) throw new IllegalArgumentException("Multiple transform source slots");
                source = index;
            }
            index += amount;
        }
        if (section.contains("transform.source"))
            throw new IllegalArgumentException("Shapeless transform sources must use source: true on an ingredient");
        RecipesEventsManager.get().registerShapelessRecipe(recipe.getKey(), choices, source, transform);
    }

    private static boolean isSource(Object ingredient) {
        Object value = ingredient instanceof ConfigurationSection section ? section.get("source")
                : ingredient instanceof Map<?, ?> map ? map.get("source") : null;
        if (value != null && !(value instanceof Boolean))
            throw new IllegalArgumentException("Ingredient source must be true or false");
        return Boolean.TRUE.equals(value);
    }

    public record Match(ItemStack source) {}

    public interface Matcher {
        Match match(ItemStack[] matrix);
    }

    public static Matcher shaped(List<String> shape, Map<Character, RecipeChoice> ingredients, Character source) {
        List<String> rows = trimmedShape(shape);
        Map<Character, RecipeChoice> choices = new HashMap<>();
        ingredients.forEach((symbol, choice) -> choices.put(symbol, choice.clone()));
        if (rows.isEmpty() || rows.size() > 3) throw new IllegalArgumentException("Invalid crafting shape");
        int width = rows.getFirst().length();
        if (width == 0 || width > 3 || rows.stream().anyMatch(row -> row.length() != width))
            throw new IllegalArgumentException("Invalid crafting shape");
        int sources = 0;
        for (String row : rows) for (char symbol : row.toCharArray()) {
            if (symbol != ' ' && !choices.containsKey(symbol))
                throw new IllegalArgumentException("Missing crafting ingredient " + symbol);
            if (source != null && symbol == source) sources++;
        }
        if (source != null && sources != 1)
            throw new IllegalArgumentException("A transform must have exactly one source slot");
        return matrix -> {
            int size = gridSize(matrix);
            if (size < width || size < rows.size()) return null;
            for (int y = 0; y <= size - rows.size(); y++) {
                for (int x = 0; x <= size - width; x++) {
                    for (boolean mirror : new boolean[]{false, true}) {
                        boolean matches = true;
                        ItemStack sourceItem = null;
                        for (int slot = 0; slot < matrix.length; slot++) {
                            int row = slot / size - y, column = slot % size - x;
                            char symbol = row >= 0 && row < rows.size() && column >= 0 && column < width
                                    ? rows.get(row).charAt(mirror ? width - column - 1 : column) : ' ';
                            ItemStack item = matrix[slot];
                            if (symbol == ' ' ? !empty(item) : empty(item) || !choices.get(symbol).test(item)) {
                                matches = false;
                                break;
                            }
                            if (source != null && symbol == source) sourceItem = item;
                        }
                        if (matches) return new Match(sourceItem);
                    }
                }
            }
            return null;
        };
    }

    public static Matcher shapeless(List<RecipeChoice> ingredients, int sourceIndex) {
        List<RecipeChoice> choices = ingredients.stream().map(RecipeChoice::clone).toList();
        if (choices.isEmpty() || choices.size() > 9 || sourceIndex < -1 || sourceIndex >= choices.size())
            throw new IllegalArgumentException("Invalid shapeless ingredients or transform source");
        return matrix -> {
            if (gridSize(matrix) == 0) return null;
            List<ItemStack> items = Arrays.stream(matrix).filter(item -> !empty(item)).toList();
            if (items.size() != choices.size()) return null;
            int[] assignment = new int[choices.size()];
            if (!assign(choices, items, 0, new boolean[items.size()], assignment)) return null;
            return new Match(sourceIndex < 0 ? null : items.get(assignment[sourceIndex]));
        };
    }

    private static List<String> trimmedShape(List<String> shape) {
        if (shape.isEmpty() || shape.size() > 3) throw new IllegalArgumentException("Invalid crafting shape");
        int width = shape.getFirst().length();
        if (width < 1 || width > 3 || shape.stream().anyMatch(row -> row.length() != width))
            throw new IllegalArgumentException("Invalid crafting shape");
        int top = 3, left = 3, bottom = -1, right = -1;
        for (int row = 0; row < shape.size(); row++) for (int column = 0; column < width; column++) {
            if (shape.get(row).charAt(column) == ' ') continue;
            top = Math.min(top, row);
            left = Math.min(left, column);
            bottom = Math.max(bottom, row);
            right = Math.max(right, column);
        }
        if (bottom < 0) throw new IllegalArgumentException("Crafting shape has no ingredients");
        List<String> rows = new ArrayList<>();
        for (int row = top; row <= bottom; row++) rows.add(shape.get(row).substring(left, right + 1));
        return List.copyOf(rows);
    }

    private static boolean assign(List<RecipeChoice> choices, List<ItemStack> items, int index,
                                  boolean[] used, int[] assignment) {
        if (index == choices.size()) return true;
        for (int i = 0; i < items.size(); i++) {
            if (used[i] || !choices.get(index).test(items.get(i))) continue;
            used[i] = true;
            assignment[index] = i;
            if (assign(choices, items, index + 1, used, assignment)) return true;
            used[i] = false;
        }
        return false;
    }

    private static int gridSize(ItemStack[] matrix) {
        return matrix == null ? 0 : matrix.length == 4 ? 2 : matrix.length == 9 ? 3 : 0;
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.isEmpty();
    }
}
