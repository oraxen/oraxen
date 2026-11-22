package io.th0rgal.oraxen.recipes;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import java.util.Map;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeTransformsTest {
    @Test
    void identityAndModelsCannotBeCopied() {
        for (String name : List.of("custom_data", "custom_model_data", "item_model", "minecraft:custom_data"))
            assertThrows(IllegalArgumentException.class, () -> RecipeTransforms.keeping(List.of(name)));
    }

    @Test
    void unknownComponentsAreRejectedRatherThanSilentlyIgnored() {
        assertThrows(IllegalArgumentException.class, () -> RecipeTransforms.keeping(List.of("unknown_component")));
    }

    @Test
    void emptyKeepListClonesResultWithoutReplacingMetadata() {
        ItemStack source = mock(ItemStack.class), result = mock(ItemStack.class), output = mock(ItemStack.class);
        when(result.clone()).thenReturn(output);
        assertSame(output, RecipeTransforms.keeping(List.of()).apply(source, result));
        verifyNoInteractions(source, output);
    }

    @Test
    void enchantmentsComeFromSourceWithoutReplacingResultIdentityOrModel() {
        ItemStack source = mock(ItemStack.class), result = mock(ItemStack.class), output = mock(ItemStack.class);
        ItemMeta sourceMeta = mock(ItemMeta.class), resultMeta = mock(ItemMeta.class);
        Enchantment enchantment = mock(Enchantment.class);
        when(result.clone()).thenReturn(output);
        when(source.getItemMeta()).thenReturn(sourceMeta);
        when(output.getItemMeta()).thenReturn(resultMeta);
        when(sourceMeta.getEnchants()).thenReturn(Map.of(enchantment, 4));
        when(resultMeta.getEnchants()).thenReturn(Map.of());
        assertSame(output, RecipeTransforms.keeping(List.of("minecraft:enchantments")).apply(source, result));
        verify(resultMeta).addEnchant(enchantment, 4, true);
        verify(output).setItemMeta(resultMeta);
        verify(resultMeta, never()).setCustomModelData(any());
        verify(sourceMeta, never()).getPersistentDataContainer();
        verify(result, never()).setItemMeta(any());
    }

    @Test
    void transformRequiresAnExplicitKeepList() {
        MemoryConfiguration config = new MemoryConfiguration();
        assertNull(RecipeTransforms.parse(config));
        config.set("transform.source", "base");
        assertThrows(IllegalArgumentException.class, () -> RecipeTransforms.parse(config));
    }
}
