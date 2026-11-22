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
import io.th0rgal.oraxen.recipes.RecipeCrafting;
import io.th0rgal.oraxen.recipes.RecipeTransforms;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
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
    private volatile Map<NamespacedKey, SmithingRegistration> smithingRecipes = Map.of();
    private Map<NamespacedKey, SmithingRegistration> stagedSmithingRecipes;
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
        // Keep newer event types out of this listener so it still loads on 1.20.1.
        try {
            Class.forName("org.bukkit.event.block.CrafterCraftEvent");
            Bukkit.getPluginManager().registerEvents(new CrafterRecipeEvents(), OraxenPlugin.get());
        } catch (ClassNotFoundException ignored) {
            // Crafters are unavailable on this server version.
        }
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
        Recipe recipe = resolveCrafting(event.getRecipe(), event.getInventory().getMatrix(), recipeState);
        CustomRecipe customRecipe = CustomRecipe.fromRecipe(recipe);
        if (!matchesRegisteredOraxenIngredients(recipeState, recipe, event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
            return;
        }
        Player player = InventoryUtils.playerFromView(event);
        if (!hasPermission(player, customRecipe, recipeState)) event.getInventory().setResult(null);

        ItemStack result = event.getInventory().getResult();
        if (result == null) return;

        CraftingRegistration registration = registration(recipeState, recipe);
        if (registration != null) {
            RecipeCrafting.Match match = registration.matcher().match(event.getInventory().getMatrix());
            if (match == null) event.getInventory().setResult(null);
            else if (registration.transform() != null) {
                try {
                    event.getInventory().setResult(withBackpackContents(registration.transform().apply(match.source(), recipe.getResult()),
                            event.getInventory().getMatrix()));
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    event.getInventory().setResult(null);
                }
            }
            else {
                event.getInventory().setResult(recipe.getResult().clone());
                persistBackpackContents(event);
            }
            return;
        }

        boolean containsOraxenItem = Arrays.stream(event.getInventory().getMatrix()).anyMatch(OraxenItems::exists);
        if (!containsOraxenItem || recipe == null) return;

        if (containsRestrictedCraftingIngredient(event.getInventory().getMatrix())) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        RecipeState state = recipes;
        Recipe recipe = resolveCrafting(event.getRecipe(), event.getInventory().getMatrix(), state);
        if (!hasPermission(event.getWhoClicked(), CustomRecipe.fromRecipe(recipe), state)
                || !matchesRegisteredOraxenIngredients(state, recipe, event.getInventory().getMatrix())
                || registration(state, recipe) == null
                && containsRestrictedCraftingIngredient(event.getInventory().getMatrix())) {
            event.setCancelled(true);
            return;
        }
        CraftingRegistration registration = registration(state, recipe);
        RecipeCrafting.Match match = registration == null ? null : registration.matcher().match(event.getInventory().getMatrix());
        if (match != null && match.counted()) {
            CountedCrafting.take(event, registration.matcher(), match, registration.transform() == null,
                    (matched, matrix) -> withBackpackContents(registeredResult(recipe, registration, matched), matrix));
            return;
        }
        if (registration != null && registration.transform() == null && recipe != event.getRecipe())
            event.setCurrentItem(withBackpackContents(recipe.getResult(), event.getInventory().getMatrix()));
        // One transformed output depends on one source. Vanilla bulk crafting would reuse it.
        if (registration != null && registration.transform() != null) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }
            try {
                event.setCurrentItem(withBackpackContents(registration.transform().apply(match.source(), recipe.getResult()),
                        event.getInventory().getMatrix()));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                event.setCancelled(true);
                event.getInventory().setResult(null);
            }
        }
    }

    public boolean prepareCrafter(Recipe recipe, ItemStack[] matrix) {
        RecipeState state = recipes;
        return canCraftWithCrafter(resolveCrafting(recipe, matrix, state), matrix, state);
    }

    private boolean canCraftWithCrafter(Recipe recipe, ItemStack[] matrix, RecipeState state) {
        CustomRecipe custom = CustomRecipe.fromRecipe(recipe);
        if (custom != null && state.permissionsPerRecipe().containsKey(custom.getKey())) return false;
        if (!matchesRegisteredOraxenIngredients(state, recipe, matrix)) return false;
        return registration(state, recipe) != null || !containsRestrictedCraftingIngredient(matrix);
    }

    public ItemStack craftingResult(Recipe recipe, ItemStack[] matrix, ItemStack result) {
        return craftingResult(recipe, matrix, result, null, null);
    }

    ItemStack craftWithCrafter(Recipe recipe, Block block, Inventory inventory, ItemStack result) {
        return craftingResult(recipe, inventory.getContents(), result, block, inventory);
    }

    private ItemStack craftingResult(Recipe selected, ItemStack[] matrix, ItemStack result, Block block, Inventory crafter) {
        RecipeState state = recipes;
        Recipe recipe = resolveCrafting(selected, matrix, state);
        if (!canCraftWithCrafter(recipe, matrix, state)) return null;
        CraftingRegistration registration = registration(state, recipe);
        if (registration == null) return result;
        RecipeCrafting.Match match = registration.matcher().match(matrix);
        if (match == null) return null;
        if (recipe != selected || registration.transform() != null) result = registeredResult(recipe, registration, match);
        if (crafter != null && result != null && !result.isEmpty() && match.counted())
            CountedCrafting.consumeCrafterExtras(block, crafter, matrix, match);
        return result;
    }

    private static ItemStack registeredResult(Recipe recipe, CraftingRegistration registration, RecipeCrafting.Match match) {
        return registration.transform() == null ? recipe.getResult().clone()
                : registration.transform().apply(match.source(), recipe.getResult());
    }

    private static Recipe resolveCrafting(Recipe selected, ItemStack[] matrix, RecipeState state) {
        CraftingRegistration current = registration(state, selected);
        if (current == null || current.matcher().match(matrix) != null) return selected;
        for (Map.Entry<NamespacedKey, CraftingRegistration> entry : state.craftingRecipes().entrySet()) {
            if (entry.getValue().matcher().match(matrix) == null) continue;
            Recipe candidate = Bukkit.getRecipe(entry.getKey());
            if (candidate != null) return candidate;
        }
        return selected;
    }

    private static CraftingRegistration registration(RecipeState state, Recipe recipe) {
        return recipe instanceof Keyed keyed ? state.craftingRecipes().get(keyed.getKey()) : null;
    }

    static boolean containsRestrictedCraftingIngredient(ItemStack[] ingredients) {
        MiscMechanicFactory factory = MiscMechanicFactory.get();
        if (factory == null) return false;
        for (ItemStack item : ingredients) {
            if (item == null || item.isEmpty()) continue;
            MiscMechanic mechanic = factory.getMechanic(item);
            if (mechanic != null && !mechanic.isAllowedInVanillaRecipes()) return true;
        }
        return false;
    }

    private void persistBackpackContents(PrepareItemCraftEvent event) {
        event.getInventory().setResult(withBackpackContents(event.getInventory().getResult(), event.getInventory().getMatrix()));
    }

    private ItemStack withBackpackContents(ItemStack result, ItemStack[] matrix) {
        if (!hasBackpackMechanic(result)) return result;

        for (ItemStack ingredient : matrix) {
            if (!hasBackpackMechanic(ingredient) || !ingredient.hasItemMeta()) continue;
            ItemMeta ingredientMeta = ingredient.getItemMeta();
            if (ingredientMeta == null) continue;

            ItemStack[] contents = ingredientMeta.getPersistentDataContainer().get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY);
            if (contents == null) continue;

            ItemStack persistedResult = result.clone();
            ItemMeta resultMeta = persistedResult.getItemMeta();
            if (resultMeta == null) return result;
            resultMeta.getPersistentDataContainer().set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, contents);
            persistedResult.setItemMeta(resultMeta);
            return persistedResult;
        }
        return result;
    }

    private boolean hasBackpackMechanic(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        MechanicFactory backpackFactory = MechanicsManager.getMechanicFactory("backpack");
        return backpackFactory != null && backpackFactory.getMechanic(OraxenItems.getIdByItem(item)) != null;
    }

    private boolean matchesRegisteredOraxenIngredients(RecipeState recipeState, Recipe recipe, ItemStack[] matrix) {
        CraftingRegistration registration = registration(recipeState, recipe);
        return registration == null || registration.matcher().match(matrix) != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        discoverRecipes(event.getPlayer());
    }

    public void discoverRecipes(Player player) {
        if (!Settings.ADD_RECIPES_TO_BOOK.toBool()) return;
        player.discoverRecipes(getPermittedRecipes(player).stream().map(r -> NamespacedKey.fromString(r.getName(), OraxenPlugin.get())).collect(Collectors.toSet()));
    }

    public void resetRecipes() {
        stagedRecipes = new StagedRecipeState();
    }

    public void retainRecipes(Set<NamespacedKey> keys) {
        StagedRecipeState staging = stagingRecipes();
        recipes.permissionsPerRecipe().forEach((key, value) -> {
            if (keys.contains(key)) staging.permissionsPerRecipe.put(key, value);
        });
        recipes.craftingRecipes().forEach((key, value) -> {
            if (keys.contains(key)) staging.craftingRecipes.put(key, value);
        });
        for (CustomRecipe recipe : recipes.whitelistedCraftRecipesOrdered()) {
            if (keys.contains(recipe.getKey())) whitelistRecipe(recipe);
        }
        smithingRecipes.forEach((key, value) -> {
            if (keys.contains(key)) stagedSmithingRecipes.put(key, value);
        });
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
        stagingRecipes().permissionsPerRecipe.put(recipe.getKey(), permission);
    }

    public void beginSmithingReload() {
        stagedSmithingRecipes = new LinkedHashMap<>();
    }

    public void registerSmithingRecipe(NamespacedKey key, String permission) {
        if (stagedSmithingRecipes == null) throw new IllegalStateException("Smithing recipe reload has not begun");
        registerSmithingRecipe(key, permission, null);
    }

    public void registerSmithingRecipe(NamespacedKey key, String permission, RecipeTransforms.Transform transform) {
        if (stagedSmithingRecipes == null) throw new IllegalStateException("Smithing recipe reload has not begun");
        stagedSmithingRecipes.put(key, new SmithingRegistration(permission, transform, null, null, null, null, "base"));
    }

    public void registerSmithingRecipe(SmithingTransformRecipe recipe, String permission,
                                       RecipeTransforms.Transform transform) {
        if (stagedSmithingRecipes == null) throw new IllegalStateException("Smithing recipe reload has not begun");
        stagedSmithingRecipes.put(recipe.getKey(), new SmithingRegistration(permission, transform,
                recipe.getTemplate().clone(), recipe.getBase().clone(), recipe.getAddition().clone(), recipe.getResult().clone(), "base"));
    }

    public void registerSmithingRecipe(NamespacedKey key, String permission, RecipeChoice template,
                                       RecipeChoice base, RecipeChoice addition,
                                       RecipeTransforms.Transform transform, String source) {
        if (stagedSmithingRecipes == null) throw new IllegalStateException("Smithing recipe reload has not begun");
        if (!Set.of("base", "template", "addition").contains(source))
            throw new IllegalArgumentException("Invalid smithing transform source " + source);
        stagedSmithingRecipes.put(key, new SmithingRegistration(permission, transform,
                template.clone(), base.clone(), addition.clone(), null, source));
    }

    public SmithingTransformRecipe resolveSmithing(SmithingTransformRecipe selected, ItemStack template,
                                                    ItemStack base, ItemStack addition) {
        if (matchesSmithing(selected.getKey(), template, base, addition)) return selected;
        for (NamespacedKey key : smithingRecipes.keySet()) {
            if (!matchesSmithing(key, template, base, addition)) continue;
            if (Bukkit.getRecipe(key) instanceof SmithingTransformRecipe candidate) return candidate;
        }
        return selected;
    }

    public boolean matchesSmithing(NamespacedKey key, ItemStack template, ItemStack base, ItemStack addition) {
        SmithingRegistration registration = smithingRecipes.get(key);
        return registration == null || registration.template() == null
                || registration.template().test(template) && registration.base().test(base) && registration.addition().test(addition);
    }

    public void finishSmithingReload() {
        smithingRecipes = stagedSmithingRecipes == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(stagedSmithingRecipes));
        stagedSmithingRecipes = null;
    }

    public void cancelSmithingReload() {
        stagedSmithingRecipes = null;
    }

    public boolean isConfiguredSmithingRecipe(NamespacedKey key) {
        return smithingRecipes.containsKey(key);
    }

    public boolean hasSmithingPermission(CommandSender sender, NamespacedKey key) {
        SmithingRegistration registration = smithingRecipes.get(key);
        return registration == null || registration.permission() == null || sender.hasPermission(registration.permission());
    }

    public ItemStack smithingResult(NamespacedKey key, ItemStack template, ItemStack base,
                                    ItemStack addition, ItemStack result) {
        SmithingRegistration registration = smithingRecipes.get(key);
        ItemStack source = registration == null ? base : switch (registration.source()) {
            case "template" -> template;
            case "addition" -> addition;
            default -> base;
        };
        return smithingResult(key, source, result);
    }

    public ItemStack smithingResult(NamespacedKey key, ItemStack source, ItemStack result) {
        SmithingRegistration registration = smithingRecipes.get(key);
        if (registration == null) return result;
        ItemStack configured = registration.result() == null ? result : registration.result().clone();
        return registration.transform() == null ? configured : registration.transform().apply(source, configured);
    }

    public boolean hasSmithingTransform(NamespacedKey key) {
        SmithingRegistration registration = smithingRecipes.get(key);
        return registration != null && registration.transform() != null;
    }

    public void registerShapedRecipe(NamespacedKey key, List<String> shape, Map<Character, RecipeChoice> choices,
                                     Character source, RecipeTransforms.Transform transform) {
        registerShapedRecipe(key, shape, choices, Map.of(), source, transform);
    }

    public void registerShapedRecipe(NamespacedKey key, List<String> shape, Map<Character, RecipeChoice> choices,
                                     Map<Character, Integer> amounts, Character source, RecipeTransforms.Transform transform) {
        if (transform != null && source == null) throw new IllegalArgumentException("Missing transform source");
        stagingRecipes().craftingRecipes.put(key,
                new CraftingRegistration(RecipeCrafting.shaped(shape, choices, amounts, source), transform));
    }

    public void registerShapelessRecipe(NamespacedKey key, List<RecipeChoice> choices, int sourceIndex,
                                        RecipeTransforms.Transform transform) {
        registerShapelessRecipe(key, choices, Collections.nCopies(choices.size(), 1), sourceIndex, transform);
    }

    public void registerShapelessRecipe(NamespacedKey key, List<RecipeChoice> choices, List<Integer> amounts,
                                        int sourceIndex, RecipeTransforms.Transform transform) {
        if (transform != null && sourceIndex < 0) throw new IllegalArgumentException("Missing transform source");
        stagingRecipes().craftingRecipes.put(key,
                new CraftingRegistration(RecipeCrafting.shapeless(choices, amounts, sourceIndex), transform));
    }

    public void whitelistRecipe(CustomRecipe recipe) {
        StagedRecipeState staging = stagingRecipes();
        staging.whitelistedCraftRecipes.add(recipe);
        staging.whitelistedCraftRecipesOrdered.add(recipe);
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

        String permission = recipeState.permissionsPerRecipe().get(recipe.getKey());
        return permission == null || sender != null && sender.hasPermission(permission);
    }

    private StagedRecipeState stagingRecipes() {
        if (stagedRecipes == null) throw new IllegalStateException("Recipe reload has not begun");
        return stagedRecipes;
    }

    private record CraftingRegistration(RecipeCrafting.Matcher matcher, RecipeTransforms.Transform transform) {}
    private record SmithingRegistration(String permission, RecipeTransforms.Transform transform,
                                        RecipeChoice template, RecipeChoice base, RecipeChoice addition, ItemStack result, String source) {}

    private record RecipeState(Map<NamespacedKey, String> permissionsPerRecipe,
                               Set<CustomRecipe> whitelistedCraftRecipes,
                               List<CustomRecipe> whitelistedCraftRecipesOrdered,
                               Map<NamespacedKey, CraftingRegistration> craftingRecipes) {
        private static RecipeState empty() {
            return new RecipeState(Map.of(), Set.of(), List.of(), Map.of());
        }
    }

    private static final class StagedRecipeState {
        private final Map<NamespacedKey, CraftingRegistration> craftingRecipes = new LinkedHashMap<>();
        private final Map<NamespacedKey, String> permissionsPerRecipe = new HashMap<>();
        private final Set<CustomRecipe> whitelistedCraftRecipes = new HashSet<>();
        private final List<CustomRecipe> whitelistedCraftRecipesOrdered = new ArrayList<>();

        private RecipeState freeze() {
            return new RecipeState(Map.copyOf(permissionsPerRecipe), Set.copyOf(whitelistedCraftRecipes),
                    List.copyOf(whitelistedCraftRecipesOrdered), Collections.unmodifiableMap(new LinkedHashMap<>(craftingRecipes)));
        }
    }

}
