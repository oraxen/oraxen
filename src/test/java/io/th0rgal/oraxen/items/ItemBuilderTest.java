package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

// This test swaps the global OraxenPlugin singleton, so it must not run
// concurrently with other tests; @Isolated forces serial execution.
@Isolated
class ItemBuilderTest {

    private Field pluginInstanceField;
    private Object previousPlugin;

    @BeforeEach
    void savePluginSingleton() throws Exception {
        pluginInstanceField = OraxenPlugin.class.getDeclaredField("oraxen");
        pluginInstanceField.setAccessible(true);
        previousPlugin = pluginInstanceField.get(null);
    }

    @AfterEach
    void restorePluginSingleton() throws Exception {
        pluginInstanceField.set(null, previousPlugin);
    }

    @Test
    void changingTypeToPotionWithoutStoredEffectsDoesNotFail() throws Exception {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        doReturn("oraxen").when(plugin).namespace();

        ItemStack itemStack = mock(ItemStack.class);
        ItemMeta originalMeta = mock(ItemMeta.class);
        PotionMeta potionMeta = mock(PotionMeta.class);
        PersistentDataContainer persistentDataContainer = mock(PersistentDataContainer.class);
        AtomicReference<Material> type = new AtomicReference<>(Material.PAPER);

        when(itemStack.getType()).thenAnswer(invocation -> type.get());
        when(itemStack.getAmount()).thenReturn(1);
        doAnswer(invocation -> {
            type.set(invocation.getArgument(0));
            return null;
        }).when(itemStack).setType(any(Material.class));
        when(itemStack.getItemMeta()).thenAnswer(invocation ->
                type.get() == Material.POTION ? potionMeta : originalMeta);

        when(originalMeta.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(originalMeta.getItemFlags()).thenReturn(Set.of());
        when(potionMeta.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(potionMeta.getItemFlags()).thenReturn(Set.of());
        when(potionMeta.getCustomEffects()).thenReturn(List.of());
        when(persistentDataContainer.has(any(NamespacedKey.class), any(PersistentDataType.class))).thenReturn(false);

        NMSHandler nmsHandler = mock(NMSHandler.class, CALLS_REAL_METHODS);
        Field pluginFile = JavaPlugin.class.getDeclaredField("file");
        pluginFile.setAccessible(true);
        pluginFile.set(plugin, new File("missing-oraxen-test.jar"));

        pluginInstanceField.set(null, plugin);
        try (var mockedVersions = mockStatic(VersionUtil.class);
             var mockedNmsHandlers = mockStatic(NMSHandlers.class)) {
            mockedVersions.when(() -> VersionUtil.atOrAbove(anyString())).thenReturn(false);
            mockedNmsHandlers.when(NMSHandlers::getHandler).thenReturn(nmsHandler);

            ItemBuilder builder = new ItemBuilder(itemStack).setType(Material.POTION);

            assertDoesNotThrow(builder::regen);
        }
    }
}
