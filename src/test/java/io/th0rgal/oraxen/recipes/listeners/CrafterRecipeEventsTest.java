package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.CrafterInventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class CrafterRecipeEventsTest {

    @Test
    void restrictedIngredientCancelsAutomaticCrafting() {
        checkCraft(false, true, true);
    }

    @Test
    void explicitlyAllowedIngredientCanBeCrafted() {
        checkCraft(true, true, false);
    }

    @Test
    void ordinaryIngredientCanBeCrafted() {
        checkCraft(false, false, false);
    }

    @Test
    void disabledMiscMechanicDoesNotBlockCrafting() {
        try (MockedStatic<MiscMechanicFactory> factories = mockStatic(MiscMechanicFactory.class)) {
            CrafterCraftEvent event = event(new ItemStack[9]);
            new CrafterRecipeEvents().onCraft(event);
            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    private void checkCraft(boolean allowed, boolean hasMechanic, boolean cancelled) {
        MiscMechanicFactory factory = mock(MiscMechanicFactory.class);
        ItemStack ingredient = mock(ItemStack.class);
        if (hasMechanic) {
            MiscMechanic mechanic = mock(MiscMechanic.class);
            when(factory.getMechanic(ingredient)).thenReturn(mechanic);
            when(mechanic.isAllowedInVanillaRecipes()).thenReturn(allowed);
        }
        ItemStack[] contents = new ItemStack[9];
        // A restricted input must be detected even in the final slot among empty slots.
        contents[8] = ingredient;
        try (MockedStatic<MiscMechanicFactory> factories = mockStatic(MiscMechanicFactory.class)) {
            factories.when(MiscMechanicFactory::get).thenReturn(factory);
            CrafterCraftEvent event = event(contents);
            new CrafterRecipeEvents().onCraft(event);
            if (cancelled) verify(event).setCancelled(true);
            else verify(event, never()).setCancelled(anyBoolean());
        }
    }

    private CrafterCraftEvent event(ItemStack[] contents) {
        CrafterCraftEvent event = mock(CrafterCraftEvent.class);
        Block block = mock(Block.class);
        Crafter crafter = mock(Crafter.class);
        CrafterInventory inventory = mock(CrafterInventory.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getState()).thenReturn(crafter);
        when(crafter.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(contents);
        return event;
    }
}
