package io.th0rgal.oraxen.recipes.listeners;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

import static io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic.BACKPACK_KEY;

public class RecipesEventsManager implements Listener {

    private static RecipesEventsManager instance;
    // Recipe reloads are staged and published as one immutable generation so Folia region
    // threads never observe partially registered permissions or ingredient matchers.
    private volatile RecipeState recipes = RecipeState.empty();
    private StagedRecipeState stagedRecipes;
    private volatile Map<NamespacedKey, String> smithingRecipes = Map.of();
    private Map<NamespacedKey, String> stagedSmithingRecipes;
    private boolean eventsRegistered;

    public static RecipesEventsManager get() {
        if (instance == null) {
            instance = new RecipesEventsManager();
        }
        return instance;
    }

    public void registerEvents() {
        if (eventsRegistered) return;
        Bukkit.getPluginManager().registerEvents(instance, OraxenPlugin.get());
        Bukkit.getPluginManager().registerEvents(new SmithingRecipeEvents(), OraxenPlugin.get());
        Bukkit.getPluginManager().registerEvents(new CustomWorkstationEvents(), OraxenPlugin.get());
        eventsRegistered = true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof MerchantInventory merchantInventory)) return;
        if (event.getSlot() != 2 || merchantInventory.getSelectedRecipe() == null) return;

        String first = OraxenItems.getIdByItem(merchantInventory.getItem(0)), second = OraxenItems.getIdByItem(merchantInventory.getItem(1));
        ArrayList<ItemStack> ingredients = new ArrayList<>(merchantInventory.getSelectedRecipe().getIngredients());
        String firstIngredient = ingredients.isEmpty() ? null : OraxenItems.getIdByItem(ingredients.get(0));
        String secondIngredient = ingredients.size() < 2 ? null : OraxenItems.getIdByItem(ingredients.get(1));
        if (!Objects.equals(first, firstIngredient) || !Objects.equals(second, secondIngredient)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCrafted(PrepareItemCraftEvent event) {
        RecipeState recipeState = recipes;
        Recipe recipe = event.getRecipe();
        CustomRecipe customRecipe = CustomRecipe.fromRecipe(recipe);
        if (!matchesRegisteredOraxenIngredients(recipeState, recipe, event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
            return;
        }
        Player player = InventoryUtils.playerFromView(event);
        if (!hasPermission(player, customRecipe, recipeState)) event.getInventory().setResult(null);

        ItemStack result = event.getInventory().getResult();
        if (result == null) return;

        boolean containsOraxenItem = Arrays.stream(event.getInventory().getMatrix()).anyMatch(OraxenItems::exists);
        if (!containsOraxenItem || recipe == null) return;

        if (Arrays.stream(event.getInventory().getMatrix()).anyMatch(item -> {
            if (MiscMechanicFactory.get() == null) return false;
            MiscMechanic mechanic = MiscMechanicFactory.get().getMechanic(item);
            return mechanic != null && !mechanic.isAllowedInVanillaRecipes();
        })) {
            event.getInventory().setResult(null);
            return;
        }

        if (customRecipe == null || recipeState.whitelistedCraftRecipes().contains(customRecipe) || customRecipe.isValidDyeRecipe()) {
            persistBackpackContents(event);
            return;
        }

        event.getInventory().setResult(customRecipe.getResult());
        persistBackpackContents(event);
    }

    private void persistBackpackContents(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (!hasBackpackMechanic(result)) return;

        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (!hasBackpackMechanic(ingredient) || !ingredient.hasItemMeta()) continue;
            ItemMeta ingredientMeta = ingredient.getItemMeta();
            if (ingredientMeta == null) continue;

            ItemStack[] contents = ingredientMeta.getPersistentDataContainer().get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY);
            if (contents == null) continue;

            ItemStack persistedResult = result.clone();
            ItemMeta resultMeta = persistedResult.getItemMeta();
            if (resultMeta == null) return;
            resultMeta.getPersistentDataContainer().set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, contents);
            persistedResult.setItemMeta(resultMeta);
            event.getInventory().setResult(persistedResult);
            return;
        }
    }

    private boolean hasBackpackMechanic(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        MechanicFactory backpackFactory = MechanicsManager.getMechanicFactory("backpack");
        return backpackFactory != null && backpackFactory.getMechanic(OraxenItems.getIdByItem(item)) != null;
    }

    private boolean matchesRegisteredOraxenIngredients(RecipeState recipeState, Recipe recipe, ItemStack[] matrix) {
        if (!(recipe instanceof Keyed keyed) || !keyed.getKey().getNamespace().equals(OraxenPlugin.get().getName().toLowerCase(Locale.ROOT)))
            return true;

        String recipeName = keyed.getKey().getKey();
        ShapedOraxenRecipe shapedRecipe = recipeState.shapedOraxenIngredients().get(recipeName);
        if (shapedRecipe != null) return shapedRecipe.matches(matrix);

        List<String> shapelessIngredients = recipeState.shapelessOraxenIngredients().get(recipeName);
        if (shapelessIngredients != null) {
            List<String> remainingIngredients = new ArrayList<>(shapelessIngredients);
            for (ItemStack itemStack : matrix) {
                String itemId = OraxenItems.getIdByItem(itemStack);
                if (itemId != null) remainingIngredients.remove(itemId);
            }
            return remainingIngredients.isEmpty();
        }

        return true;
    }

    private record ShapedOraxenRecipe(int width, int height, Map<Integer, String> ingredients) {

        private boolean matches(ItemStack[] matrix) {
            int matrixWidth = switch (matrix.length) {
                case 4 -> 2;
                case 9 -> 3;
                default -> (int) Math.sqrt(matrix.length);
            };
            if (matrixWidth <= 0 || matrix.length % matrixWidth != 0) return false;

            int matrixHeight = matrix.length / matrixWidth;
            if (width > matrixWidth || height > matrixHeight) return false;

            for (int rowOffset = 0; rowOffset <= matrixHeight - height; rowOffset++) {
                for (int columnOffset = 0; columnOffset <= matrixWidth - width; columnOffset++) {
                    if (matches(matrix, matrixWidth, rowOffset, columnOffset)) return true;
                }
            }
            return false;
        }

        private boolean matches(ItemStack[] matrix, int matrixWidth, int rowOffset, int columnOffset) {
            for (Map.Entry<Integer, String> ingredient : ingredients.entrySet()) {
                int row = ingredient.getKey() / width;
                int column = ingredient.getKey() % width;
                int matrixIndex = (row + rowOffset) * matrixWidth + column + columnOffset;
                if (!Objects.equals(OraxenItems.getIdByItem(matrix[matrixIndex]), ingredient.getValue())) return false;
            }
            return true;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!Settings.ADD_RECIPES_TO_BOOK.toBool()) return;
        Player player = event.getPlayer();
        player.discoverRecipes(getPermittedRecipes(player).stream().map(r -> NamespacedKey.fromString(r.getName(), OraxenPlugin.get())).collect(Collectors.toSet()));
    }

    public void resetRecipes() {
        stagedRecipes = new StagedRecipeState();
    }

    public void finishRecipeReload() {
        if (stagedRecipes == null) throw new IllegalStateException("Recipe reload has not begun");
        recipes = stagedRecipes.freeze();
        stagedRecipes = null;
    }

    public void cancelRecipeReload() {
        stagedRecipes = null;
    }

    public void addPermissionRecipe(CustomRecipe recipe, String permission) {
        stagingRecipes().permissionsPerRecipe.put(recipe, permission);
    }

    public void beginSmithingReload() {
        stagedSmithingRecipes = new HashMap<>();
    }

    public void registerSmithingRecipe(NamespacedKey key, String permission) {
        if (stagedSmithingRecipes == null) throw new IllegalStateException("Smithing recipe reload has not begun");
        stagedSmithingRecipes.put(key, permission);
    }

    public void finishSmithingReload() {
        smithingRecipes = stagedSmithingRecipes == null ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(stagedSmithingRecipes));
        stagedSmithingRecipes = null;
    }

    public boolean isConfiguredSmithingRecipe(NamespacedKey key) {
        return smithingRecipes.containsKey(key);
    }

    public boolean hasSmithingPermission(CommandSender sender, NamespacedKey key) {
        String permission = smithingRecipes.get(key);
        return permission == null || sender.hasPermission(permission);
    }

    public void whitelistRecipe(CustomRecipe recipe) {
        StagedRecipeState staging = stagingRecipes();
        staging.whitelistedCraftRecipes.add(recipe);
        staging.whitelistedCraftRecipesOrdered.add(recipe);
    }

    public void registerShapedOraxenRecipe(String recipeName, List<String> shape, ConfigurationSection ingredientsSection) {
        Map<Character, String> ingredientIds = new HashMap<>();
        for (String ingredientLetter : ingredientsSection.getKeys(false)) {
            ConfigurationSection ingredientSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            if (ingredientSection != null && ingredientSection.isString("oraxen_item"))
                ingredientIds.put(ingredientLetter.charAt(0), ingredientSection.getString("oraxen_item"));
        }
        if (ingredientIds.isEmpty()) return;

        int minRow = Integer.MAX_VALUE;
        int minColumn = Integer.MAX_VALUE;
        int maxRow = -1;
        int maxColumn = -1;
        for (int row = 0; row < Math.min(shape.size(), 3); row++) {
            String rowShape = shape.get(row);
            for (int column = 0; column < Math.min(rowShape.length(), 3); column++) {
                if (!ingredientIds.containsKey(rowShape.charAt(column))) continue;
                minRow = Math.min(minRow, row);
                minColumn = Math.min(minColumn, column);
                maxRow = Math.max(maxRow, row);
                maxColumn = Math.max(maxColumn, column);
            }
        }
        if (maxRow == -1) return;

        int width = maxColumn - minColumn + 1;
        int height = maxRow - minRow + 1;
        Map<Integer, String> expectedIngredients = new HashMap<>();
        for (int row = minRow; row <= maxRow; row++) {
            String rowShape = shape.get(row);
            for (int column = minColumn; column <= maxColumn && column < rowShape.length(); column++) {
                String itemId = ingredientIds.get(rowShape.charAt(column));
                if (itemId != null) expectedIngredients.put((row - minRow) * width + column - minColumn, itemId);
            }
        }
        stagingRecipes().shapedOraxenIngredients.put(
                recipeName, new ShapedOraxenRecipe(width, height, Map.copyOf(expectedIngredients)));
    }

    public void registerShapelessOraxenRecipe(String recipeName, ConfigurationSection ingredientsSection) {
        List<String> expectedIngredients = new ArrayList<>();
        for (String ingredientLetter : ingredientsSection.getKeys(false)) {
            ConfigurationSection ingredientSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            if (ingredientSection != null && ingredientSection.isString("oraxen_item")) {
                int amount = Math.max(1, ingredientSection.getInt("amount", 1));
                for (int i = 0; i < amount; i++) expectedIngredients.add(ingredientSection.getString("oraxen_item"));
            }
        }
        if (!expectedIngredients.isEmpty())
            stagingRecipes().shapelessOraxenIngredients.put(recipeName, List.copyOf(expectedIngredients));
    }

    public List<CustomRecipe> getPermittedRecipes(CommandSender sender) {
        RecipeState recipeState = recipes;
        return recipeState.whitelistedCraftRecipesOrdered()
                .stream()
                .filter(customRecipe -> hasPermission(sender, customRecipe, recipeState))
                .toList();
    }

    public String[] getPermittedRecipesName(CommandSender sender) {
        return getPermittedRecipes(sender)
                .stream()
                .map(CustomRecipe::getName)
                .toArray(String[]::new);
    }


    public boolean hasPermission(CommandSender sender, CustomRecipe recipe) {
        return hasPermission(sender, recipe, recipes);
    }

    private boolean hasPermission(CommandSender sender, CustomRecipe recipe, RecipeState recipeState) {
        if (recipe == null) return true;

        String permission = recipeState.permissionsPerRecipe().get(recipe);
        return permission == null || sender.hasPermission(permission);
    }

    private StagedRecipeState stagingRecipes() {
        if (stagedRecipes == null) throw new IllegalStateException("Recipe reload has not begun");
        return stagedRecipes;
    }

    private record RecipeState(Map<CustomRecipe, String> permissionsPerRecipe,
                               Set<CustomRecipe> whitelistedCraftRecipes,
                               List<CustomRecipe> whitelistedCraftRecipesOrdered,
                               Map<String, ShapedOraxenRecipe> shapedOraxenIngredients,
                               Map<String, List<String>> shapelessOraxenIngredients) {
        private static RecipeState empty() {
            return new RecipeState(Map.of(), Set.of(), List.of(), Map.of(), Map.of());
        }
    }

    private static final class StagedRecipeState {
        private final Map<CustomRecipe, String> permissionsPerRecipe = new HashMap<>();
        private final Set<CustomRecipe> whitelistedCraftRecipes = new HashSet<>();
        private final List<CustomRecipe> whitelistedCraftRecipesOrdered = new ArrayList<>();
        private final Map<String, ShapedOraxenRecipe> shapedOraxenIngredients = new HashMap<>();
        private final Map<String, List<String>> shapelessOraxenIngredients = new HashMap<>();

        private RecipeState freeze() {
            return new RecipeState(Map.copyOf(permissionsPerRecipe), Set.copyOf(whitelistedCraftRecipes),
                    List.copyOf(whitelistedCraftRecipesOrdered), Map.copyOf(shapedOraxenIngredients),
                    Map.copyOf(shapelessOraxenIngredients));
        }
    }

}
