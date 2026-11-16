package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShapelessBuilderTest {
    @Test
    void repeatedCustomIngredientsBecomeSeparateUnitSlotsWithoutChangingStacksOrResult() {
        ShapelessBuilder builder = mock(ShapelessBuilder.class, CALLS_REAL_METHODS);
        Inventory inventory = mock(Inventory.class);
        YamlConfiguration config = new YamlConfiguration();
        ItemStack result = mock(ItemStack.class);
        when(result.getAmount()).thenReturn(4);
        ItemStack original = mock(ItemStack.class);
        ItemStack unit = mock(ItemStack.class);
        when(original.clone()).thenReturn(unit);
        when(original.getAmount()).thenReturn(32);
        AtomicInteger amount = new AtomicInteger(32);
        when(unit.getAmount()).thenAnswer(invocation -> amount.get());
        doAnswer(invocation -> { amount.set(invocation.getArgument(0)); return null; })
                .when(unit).setAmount(anyInt());
        when(inventory.getContents()).thenReturn(new ItemStack[]{result, original, null, original});
        doReturn(inventory).when(builder).getInventory();
        doReturn(config).when(builder).getConfig();
        doNothing().when(builder).saveConfig();
        doNothing().when(builder).close();
        doAnswer(invocation -> {
            ConfigurationSection section = invocation.getArgument(0);
            ItemStack stack = invocation.getArgument(1);
            section.set("minecraft-item", stack);
            if (stack.getAmount() > 1) section.set("amount", stack.getAmount());
            return null;
        }).when(builder).setSerializedItem(any(), any());

        builder.saveRecipe("custom_recipe", "custom.permission");

        List<?> ingredients = config.getList("custom_recipe.ingredients");
        assertNotNull(ingredients);
        assertEquals(2, ingredients.size());
        for (Object ingredient : ingredients) {
            Map<?, ?> entry = (Map<?, ?>) ingredient;
            assertFalse(entry.containsKey("amount"));
            assertEquals(1, ((ItemStack) entry.get("minecraft-item")).getAmount());
            assertSame(unit, entry.get("minecraft-item"));
        }
        assertSame(result, config.get("custom_recipe.result.minecraft-item"));
        assertEquals(4, config.getInt("custom_recipe.result.amount"));
        assertEquals("custom.permission", config.getString("custom_recipe.permission"));
        verify(original, never()).setAmount(anyInt());
        verify(result, never()).setAmount(anyInt());
        verify(builder).saveConfig();
        verify(builder).close();
    }
}
