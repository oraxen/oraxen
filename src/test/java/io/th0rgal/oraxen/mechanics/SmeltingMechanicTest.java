package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.farming.smelting.SmeltingMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.smelting.SmeltingMechanicListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmeltingMechanicTest extends MechanicTestSupport {

    @Test
    void readsCookingBlacklistFromTheFactoryConfig() {
        PluginManager previous = Bukkit.getPluginManager();
        when(Bukkit.getServer().getPluginManager()).thenReturn(mock(PluginManager.class));
        try {
            SmeltingMechanicFactory factory = new SmeltingMechanicFactory(
                    standaloneSection("blacklist_cooked", List.of("wet_sponge", "KELP")));

            assertTrue(factory.isCookingBlacklisted(Material.WET_SPONGE));
            assertTrue(factory.isCookingBlacklisted(Material.KELP));
            assertFalse(factory.isCookingBlacklisted(Material.RAW_IRON));
        } finally {
            MechanicsManager.unloadListeners("test");
            when(Bukkit.getServer().getPluginManager()).thenReturn(previous);
        }
    }

    @Test
    void smeltsTheToolDropsWithFurnaceRecipesExceptBlacklistedOnes() {
        SmeltingMechanicFactory factory = mock(SmeltingMechanicFactory.class);
        when(factory.isCookingBlacklisted(Material.WET_SPONGE)).thenReturn(true);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack tool = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(tool);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        when(Bukkit.getServer().getWorld(worldId)).thenReturn(world);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(new Location(world, 1, 2, 3));
        List<ItemStack> drops = List.of(stack(Material.RAW_IRON, 40), stack(Material.WET_SPONGE, 1), stack(Material.DIRT, 1));
        when(block.getDrops(tool, player)).thenReturn(drops);
        when(Bukkit.getServer().recipeIterator()).thenAnswer(invocation -> List.<Recipe>of(mock(Recipe.class),
                furnaceRecipe(Material.RAW_IRON, stack(Material.IRON_INGOT, 2)),
                furnaceRecipe(Material.WET_SPONGE, stack(Material.SPONGE, 1))).iterator());
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getIdByItem(tool)).thenReturn("smelter");
            new SmeltingMechanicListener(factory).onBlockBreak(event);
        }

        assertFalse(event.isDropItems());
        ArgumentCaptor<ItemStack> dropped = ArgumentCaptor.forClass(ItemStack.class);
        verify(world, atLeastOnce()).dropItemNaturally(any(Location.class), dropped.capture());
        // 40 raw iron with two ingots per item make 80 ingots, split into full stacks.
        assertEquals(List.of("IRON_INGOT x64", "IRON_INGOT x16", "WET_SPONGE x1", "DIRT x1"),
                dropped.getAllValues().stream().map(drop -> drop.getType() + " x" + drop.getAmount()).toList());
        verify(block, never()).getDrops();
    }

    private static FurnaceRecipe furnaceRecipe(Material input, ItemStack result) {
        RecipeChoice choice = mock(RecipeChoice.class);
        when(choice.test(any())).thenAnswer(invocation -> invocation.<ItemStack>getArgument(0).getType() == input);
        FurnaceRecipe recipe = mock(FurnaceRecipe.class);
        when(recipe.getInputChoice()).thenReturn(choice);
        when(recipe.getResult()).thenReturn(result);
        return recipe;
    }

    private static ItemStack stack(Material type, int amount) {
        ItemStack stack = mock(ItemStack.class);
        AtomicInteger count = new AtomicInteger(amount);
        when(stack.getType()).thenReturn(type);
        when(stack.getAmount()).thenAnswer(invocation -> count.get());
        when(stack.getMaxStackSize()).thenReturn(64);
        doAnswer(invocation -> {
            count.set(invocation.getArgument(0));
            return null;
        }).when(stack).setAmount(anyInt());
        when(stack.clone()).thenAnswer(invocation -> stack(type, count.get()));
        when(stack.asQuantity(anyInt())).thenAnswer(invocation -> stack(type, invocation.getArgument(0)));
        return stack;
    }
}
