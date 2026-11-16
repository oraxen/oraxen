package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.RecipeTransforms;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.Bukkit;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.persistence.PersistentDataContainer;
import com.jeff_media.morepersistentdatatypes.DataType;

import static io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic.BACKPACK_KEY;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipesEventsManagerTest {
    private MockedStatic<OraxenPlugin> plugin;

    @BeforeEach
    void initializePlugin() {
        OraxenPlugin instance = mock(OraxenPlugin.class);
        when(instance.getName()).thenReturn("Oraxen");
        when(instance.namespace()).thenReturn("oraxen");
        plugin = mockStatic(OraxenPlugin.class);
        plugin.when(OraxenPlugin::get).thenReturn(instance);
    }

    @AfterEach
    void closePlugin() {
        plugin.close();
    }

    @Test
    void matcherPublicationIsAtomicAndRejectsSameMaterialPredicateFailures() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack accepted = item(), rejected = item();
        RecipeChoice choice = choice(accepted);
        ShapelessRecipe recipe = recipe("predicate", choice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice), -1, null);
        manager.finishRecipeReload();
        assertTrue(manager.prepareCrafter(recipe, matrix(accepted)));
        assertFalse(manager.prepareCrafter(recipe, matrix(rejected)));
        manager.resetRecipes();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice(rejected)), -1, null);
        assertTrue(manager.prepareCrafter(recipe, matrix(accepted)));
        manager.cancelRecipeReload();
        assertTrue(manager.prepareCrafter(recipe, matrix(accepted)));
    }

    @Test
    void broadServerChoicesSelectTheMatchingPaperBackedCustomRecipe() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack first = item(), second = item();
        when(first.getType()).thenReturn(Material.PAPER);
        when(second.getType()).thenReturn(Material.PAPER);
        RecipeChoice firstChoice = choice(first), secondChoice = choice(second);
        ShapelessRecipe firstRecipe = recipe("first_paper", firstChoice);
        ShapelessRecipe secondRecipe = recipe("second_paper", secondChoice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(firstRecipe.getKey(), List.of(firstChoice), -1, null);
        manager.registerShapelessRecipe(secondRecipe.getKey(), List.of(secondChoice), -1, null);
        manager.finishRecipeReload();
        assertTrue(manager.prepareCrafter(firstRecipe, matrix(first)));
        assertTrue(manager.prepareCrafter(secondRecipe, matrix(second)));
        try (var server = mockStatic(Bukkit.class)) {
            server.when(() -> Bukkit.getRecipe(firstRecipe.getKey())).thenReturn(firstRecipe);
            server.when(() -> Bukkit.getRecipe(secondRecipe.getKey())).thenReturn(secondRecipe);
            assertTrue(manager.prepareCrafter(firstRecipe, matrix(second)));
            assertTrue(manager.prepareCrafter(secondRecipe, matrix(first)));
            assertSame(secondRecipe.getResult(), manager.craftingResult(firstRecipe, matrix(second), firstRecipe.getResult()));
        }
    }

    @Test
    void permissionsUseRecipeKeysAndCannotBeBypassedByAutomaticCrafters() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack ingredient = item();
        RecipeChoice choice = choice(ingredient);
        ShapelessRecipe locked = recipe("locked", choice), unlocked = recipe("unlocked", choice);
        ShapelessRecipe vanilla = recipe("locked", choice);
        when(vanilla.getKey()).thenReturn(NamespacedKey.minecraft("locked"));
        manager.resetRecipes();
        manager.registerShapelessRecipe(locked.getKey(), List.of(choice), -1, null);
        manager.registerShapelessRecipe(unlocked.getKey(), List.of(choice), -1, null);
        manager.addPermissionRecipe(CustomRecipe.fromRecipe(locked), "oraxen.recipe.locked");
        manager.finishRecipeReload();
        CommandSender sender = mock(CommandSender.class);
        assertFalse(manager.hasPermission(sender, CustomRecipe.fromRecipe(locked)));
        assertTrue(manager.hasPermission(sender, CustomRecipe.fromRecipe(unlocked)));
        assertFalse(manager.prepareCrafter(locked, matrix(ingredient)));
        assertTrue(manager.prepareCrafter(unlocked, matrix(ingredient)));
        assertTrue(manager.hasPermission(sender, CustomRecipe.fromRecipe(vanilla)));
        assertTrue(manager.prepareCrafter(vanilla, matrix(ingredient)));
    }

    @Test
    void smithingUsesTheRegisteredPredicatesRatherThanTheServersMaterialChoices() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack template = item(), base = item(), addition = item(), rejected = item();
        NamespacedKey key = new NamespacedKey("oraxen", "smithing");
        manager.beginSmithingReload();
        manager.registerSmithingRecipe(key, null, choice(template), choice(base), choice(addition), null, "base");
        manager.finishSmithingReload();
        assertTrue(manager.matchesSmithing(key, template, base, addition));
        assertFalse(manager.matchesSmithing(key, template, rejected, addition));
        assertFalse(manager.matchesSmithing(key, rejected, base, addition));
        assertFalse(manager.matchesSmithing(key, template, base, rejected));
    }

    @Test
    void transformedShiftClicksCannotReuseTheFirstSourcesComponentsForBulkCrafts() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack ingredient = item();
        RecipeChoice choice = choice(ingredient);
        ShapelessRecipe recipe = recipe("transform", choice);
        manager.resetRecipes();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice), 0, RecipeTransforms.keeping(List.of("enchantments")));
        manager.finishRecipeReload();
        CraftItemEvent event = mock(CraftItemEvent.class);
        CraftingInventory inventory = mock(CraftingInventory.class);
        when(event.getRecipe()).thenReturn(recipe);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getMatrix()).thenReturn(matrix(ingredient));
        when(event.isShiftClick()).thenReturn(true);
        manager.onCraftItem(event);
        verify(event).setCancelled(true);
    }

    @Test
    void overlappingSmithingRecipesResolveTheMatchingCustomBase() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack template = item(), firstBase = item(), secondBase = item(), addition = item();
        SmithingTransformRecipe first = mock(SmithingTransformRecipe.class);
        SmithingTransformRecipe second = mock(SmithingTransformRecipe.class);
        when(first.getKey()).thenReturn(new NamespacedKey("oraxen", "first"));
        when(second.getKey()).thenReturn(new NamespacedKey("oraxen", "second"));
        manager.beginSmithingReload();
        manager.registerSmithingRecipe(first.getKey(), "first.permission", choice(template), choice(firstBase), choice(addition), null, "base");
        manager.registerSmithingRecipe(second.getKey(), null, choice(template), choice(secondBase), choice(addition), null, "base");
        manager.finishSmithingReload();
        ItemStack result = item();
        when(second.getResult()).thenReturn(result);
        RecipeChoice templateChoice = choice(template), baseChoice = choice(secondBase), additionChoice = choice(addition);
        when(second.getTemplate()).thenReturn(templateChoice);
        when(second.getBase()).thenReturn(baseChoice);
        when(second.getAddition()).thenReturn(additionChoice);
        SmithingInventory inventory = mock(SmithingInventory.class);
        when(inventory.getRecipe()).thenReturn(first);
        when(inventory.getInputTemplate()).thenReturn(template);
        when(inventory.getInputEquipment()).thenReturn(secondBase);
        when(inventory.getInputMineral()).thenReturn(addition);
        PrepareSmithingEvent prepare = mock(PrepareSmithingEvent.class);
        when(prepare.getInventory()).thenReturn(inventory);
        SmithItemEvent click = mock(SmithItemEvent.class);
        when(click.getInventory()).thenReturn(inventory);
        when(click.getWhoClicked()).thenReturn(mock(Player.class));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<RecipesEventsManager> managers = mockStatic(RecipesEventsManager.class);
             MockedStatic<RecipesBuilderEvents> builders = mockStatic(RecipesBuilderEvents.class);
             MockedStatic<InventoryUtils> inventories = mockStatic(InventoryUtils.class)) {
            bukkit.when(() -> Bukkit.getRecipe(second.getKey())).thenReturn(second);
            managers.when(RecipesEventsManager::get).thenReturn(manager);
            inventories.when(() -> InventoryUtils.playerFromView(prepare)).thenReturn(mock(Player.class));
            assertSame(second, manager.resolveSmithing(first, template, secondBase, addition));
            SmithingRecipeEvents listener = new SmithingRecipeEvents();
            listener.onSmithingRecipe(prepare);
            verify(prepare).setResult(result);
            listener.onSmithItem(click);
            verify(click, never()).setCancelled(true);
        }
    }

    @Test
    void backpackContentsSurviveResolvedAndTransformedCraftClicks() {
        for (boolean transformed : List.of(false, true)) {
            RecipesEventsManager manager = new RecipesEventsManager();
            ItemStack ingredient = item(), rejected = item(), result = item();
            ItemMeta ingredientMeta = mock(ItemMeta.class);
            ItemMeta resultMeta = mock(ItemMeta.class);
            PersistentDataContainer sourceData = mock(PersistentDataContainer.class);
            PersistentDataContainer resultData = mock(PersistentDataContainer.class);
            ItemStack[] contents = {item()};
            when(ingredient.hasItemMeta()).thenReturn(true);
            when(ingredient.getItemMeta()).thenReturn(ingredientMeta);
            when(ingredientMeta.getPersistentDataContainer()).thenReturn(sourceData);
            when(result.getItemMeta()).thenReturn(resultMeta);
            when(resultMeta.getPersistentDataContainer()).thenReturn(resultData);
            when(sourceData.get(BACKPACK_KEY,
                    DataType.ITEM_STACK_ARRAY)).thenReturn(contents);
            RecipeChoice accepted = choice(ingredient);
            ShapelessRecipe first = recipe("first_bag", choice(rejected));
            ShapelessRecipe second = recipe("second_bag", accepted);
            when(second.getResult()).thenReturn(result);
            manager.resetRecipes();
            manager.registerShapelessRecipe(first.getKey(), List.of(choice(rejected)), -1, null);
            manager.registerShapelessRecipe(second.getKey(), List.of(accepted), transformed ? 0 : -1,
                    transformed ? RecipeTransforms.keeping(List.of()) : null);
            manager.finishRecipeReload();
            MechanicFactory factory = mock(MechanicFactory.class);
            when(factory.getMechanic("bag")).thenReturn(mock(Mechanic.class));
            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class);
                 MockedStatic<MechanicsManager> mechanics = mockStatic(MechanicsManager.class)) {
                bukkit.when(() -> Bukkit.getRecipe(second.getKey())).thenReturn(second);
                items.when(() -> OraxenItems.getIdByItem(ingredient)).thenReturn("bag");
                items.when(() -> OraxenItems.getIdByItem(result)).thenReturn("bag");
                mechanics.when(() -> MechanicsManager.getMechanicFactory("backpack")).thenReturn(factory);
                CraftingInventory inventory = mock(CraftingInventory.class);
                when(inventory.getMatrix()).thenReturn(matrix(ingredient));
                CraftItemEvent event = mock(CraftItemEvent.class);
                when(event.getRecipe()).thenReturn(first);
                when(event.getInventory()).thenReturn(inventory);
                manager.onCraftItem(event);
                verify(event).setCurrentItem(result);
                verify(event, never()).setCancelled(true);
                verify(resultData).set(BACKPACK_KEY,
                        DataType.ITEM_STACK_ARRAY, contents);
            }
        }
    }

    @Test
    void retainedRecipesKeepTheirPermissionsAndIngredientMatchers() {
        RecipesEventsManager manager = new RecipesEventsManager();
        ItemStack ingredient = item(), rejected = item();
        RecipeChoice choice = choice(ingredient);
        ShapelessRecipe recipe = recipe("retained", choice);
        manager.resetRecipes();
        manager.beginSmithingReload();
        manager.registerShapelessRecipe(recipe.getKey(), List.of(choice), -1, null);
        manager.addPermissionRecipe(CustomRecipe.fromRecipe(recipe), "oraxen.recipe.retained");
        manager.finishRecipeReload();
        manager.finishSmithingReload();
        manager.resetRecipes();
        manager.beginSmithingReload();
        manager.retainRecipes(java.util.Set.of(recipe.getKey()));
        manager.finishRecipeReload();
        manager.finishSmithingReload();
        assertFalse(manager.hasPermission(mock(CommandSender.class), CustomRecipe.fromRecipe(recipe)));
        assertFalse(manager.prepareCrafter(recipe, matrix(rejected)));
        CommandSender permitted = mock(CommandSender.class);
        when(permitted.hasPermission("oraxen.recipe.retained")).thenReturn(true);
        assertTrue(manager.hasPermission(permitted, CustomRecipe.fromRecipe(recipe)));
    }

    private ShapelessRecipe recipe(String name, RecipeChoice choice) {
        ShapelessRecipe recipe = mock(ShapelessRecipe.class);
        when(recipe.getKey()).thenReturn(new NamespacedKey("oraxen", name));
        when(recipe.getChoiceList()).thenReturn(List.of(choice));
        ItemStack result = item();
        when(recipe.getResult()).thenReturn(result);
        return recipe;
    }

    private RecipeChoice choice(ItemStack accepted) {
        RecipeChoice choice = mock(RecipeChoice.class);
        when(choice.clone()).thenReturn(choice);
        when(choice.getItemStack()).thenReturn(accepted);
        when(choice.test(any())).thenAnswer(invocation -> invocation.getArgument(0) == accepted);
        return choice;
    }

    private ItemStack item() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(item.getAmount()).thenReturn(1);
        when(item.clone()).thenReturn(item);
        return item;
    }

    private ItemStack[] matrix(ItemStack item) {
        ItemStack[] matrix = new ItemStack[9];
        matrix[8] = item;
        return matrix;
    }
}
