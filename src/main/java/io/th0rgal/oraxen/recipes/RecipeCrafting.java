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
        Map<Character, Integer> amounts = new HashMap<>();
        if (ingredients != null) for (String symbol : ingredients.getKeys(false)) {
            Object ingredient = ingredients.get(symbol);
            amounts.put(symbol.charAt(0), RecipeIngredients.amount(ingredient));
            if (!isSource(ingredient)) continue;
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
                amounts, source, transform);
    }

    public static void registerShapeless(ShapelessRecipe recipe, ConfigurationSection section,
                                         List<RecipeChoice> choices) {
        RecipeTransforms.Transform transform = RecipeTransforms.parse(section);
        int source = -1;
        List<Integer> amounts = new ArrayList<>();
        Object raw = section.get("ingredients");
        Collection<?> ingredients = raw instanceof ConfigurationSection legacy
                ? legacy.getValues(false).values() : raw instanceof List<?> list ? list : List.of();
        for (Object ingredient : ingredients) {
            int amount = RecipeIngredients.amount(ingredient);
            if (isSource(ingredient)) {
                if (source != -1 || amount != 1) throw new IllegalArgumentException("Multiple transform source slots");
                source = amounts.size();
            }
            // Legacy keyed ingredients used amount to repeat an ingredient across slots.
            if (raw instanceof ConfigurationSection) amounts.addAll(Collections.nCopies(amount, 1));
            else amounts.add(amount);
        }
        if (section.contains("transform.source"))
            throw new IllegalArgumentException("Shapeless transform sources must use source: true on an ingredient");
        RecipesEventsManager.get().registerShapelessRecipe(recipe.getKey(), choices, amounts, source, transform);
    }

    private static boolean isSource(Object ingredient) {
        Object value = ingredient instanceof ConfigurationSection section ? section.get("source")
                : ingredient instanceof Map<?, ?> map ? map.get("source") : null;
        if (value != null && !(value instanceof Boolean))
            throw new IllegalArgumentException("Ingredient source must be true or false");
        return Boolean.TRUE.equals(value);
    }

    public record Match(ItemStack source, int[] amounts) {
        public boolean counted() {
            for (int amount : amounts) if (amount > 1) return true;
            return false;
        }
    }

    public interface Matcher {
        Match match(ItemStack[] matrix);
    }

    public static Matcher shaped(List<String> shape, Map<Character, RecipeChoice> ingredients, Character source) {
        return shaped(shape, ingredients, Map.of(), source);
    }

    public static Matcher shaped(List<String> shape, Map<Character, RecipeChoice> ingredients,
                                 Map<Character, Integer> amounts, Character source) {
        List<String> rows = trimmedShape(shape);
        Map<Character, Integer> counts = Map.copyOf(amounts);
        if (counts.values().stream().anyMatch(amount -> amount < 1))
            throw new IllegalArgumentException("Crafting ingredient amounts must be positive");
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
                        int[] used = new int[matrix.length];
                        for (int slot = 0; slot < matrix.length; slot++) {
                            int row = slot / size - y, column = slot % size - x;
                            char symbol = row >= 0 && row < rows.size() && column >= 0 && column < width
                                    ? rows.get(row).charAt(mirror ? width - column - 1 : column) : ' ';
                            ItemStack item = matrix[slot];
                            int amount = symbol == ' ' ? 0 : counts.getOrDefault(symbol, 1);
                            if (symbol == ' ' ? !empty(item) : !matches(choices.get(symbol), item, amount)) {
                                matches = false;
                                break;
                            }
                            used[slot] = amount;
                            if (source != null && symbol == source) sourceItem = item;
                        }
                        if (matches) return new Match(sourceItem, used);
                    }
                }
            }
            return null;
        };
    }

    public static Matcher shapeless(List<RecipeChoice> ingredients, int sourceIndex) {
        return shapeless(ingredients, Collections.nCopies(ingredients.size(), 1), sourceIndex);
    }

    public static Matcher shapeless(List<RecipeChoice> ingredients, List<Integer> amounts, int sourceIndex) {
        List<RecipeChoice> choices = ingredients.stream().map(RecipeChoice::clone).toList();
        List<Integer> counts = List.copyOf(amounts);
        if (choices.isEmpty() || choices.size() > 9 || counts.size() != choices.size()
                || counts.stream().anyMatch(amount -> amount < 1) || sourceIndex < -1 || sourceIndex >= choices.size())
            throw new IllegalArgumentException("Invalid shapeless ingredients or transform source");
        return matrix -> {
            if (gridSize(matrix) == 0) return null;
            int occupied = 0;
            for (ItemStack item : matrix) if (!empty(item)) occupied++;
            if (occupied != choices.size()) return null;
            int[] assignment = new int[choices.size()];
            int[] used = new int[matrix.length];
            if (!assign(choices, counts, matrix, 0, used, assignment)) return null;
            return new Match(sourceIndex < 0 ? null : matrix[assignment[sourceIndex]], used);
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

    private static boolean assign(List<RecipeChoice> choices, List<Integer> counts, ItemStack[] matrix,
                                  int index, int[] used, int[] assignment) {
        if (index == choices.size()) return true;
        int amount = counts.get(index);
        for (int slot = 0; slot < matrix.length; slot++) {
            if (used[slot] != 0 || !matches(choices.get(index), matrix[slot], amount)) continue;
            used[slot] = amount;
            assignment[index] = slot;
            if (assign(choices, counts, matrix, index + 1, used, assignment)) return true;
            used[slot] = 0;
        }
        return false;
    }

    private static boolean matches(RecipeChoice choice, ItemStack item, int amount) {
        return !empty(item) && (amount == 1 || item.getAmount() >= amount) && choice.test(item);
    }

    private static int gridSize(ItemStack[] matrix) {
        return matrix == null ? 0 : matrix.length == 4 ? 2 : matrix.length == 9 ? 3 : 0;
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.isEmpty();
    }
}
