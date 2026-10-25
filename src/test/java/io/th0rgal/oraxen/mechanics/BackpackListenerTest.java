package io.th0rgal.oraxen.mechanics;

import com.jeff_media.morepersistentdatatypes.DataType;
import dev.triumphteam.gui.builder.gui.StorageBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackListener;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanicFactory;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic.BACKPACK_KEY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackpackListenerTest extends MechanicTestSupport {

    @BeforeAll
    static void initializeGuiLibrary() throws ClassNotFoundException {
        ConfigsManager configs = mock(ConfigsManager.class);
        YamlConfiguration language = new YamlConfiguration();
        language.set("general.prefix", "");
        when(configs.getLanguage()).thenReturn(language);
        when(OraxenPlugin.get().getConfigsManager()).thenReturn(configs);
        PluginManager previous = Bukkit.getPluginManager();
        try (MockedStatic<JavaPlugin> plugins = mockStatic(JavaPlugin.class)) {
            when(Bukkit.getServer().getPluginManager()).thenReturn(mock(PluginManager.class));
            plugins.when(() -> JavaPlugin.getProvidingPlugin(BaseGui.class)).thenReturn(OraxenPlugin.get());
            Class.forName(BaseGui.class.getName());
        } finally {
            when(Bukkit.getServer().getPluginManager()).thenReturn(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    void savesWithdrawalsToRestoredBackpackAfterCancelledDrop(int selectedSlot) {
        try (BackpackFixture fixture = new BackpackFixture()) {
            fixture.open();
            Item dropped = mock(Item.class);
            when(dropped.getItemStack()).thenReturn(fixture.snapshot);
            PlayerDropItemEvent drop = new PlayerDropItemEvent(fixture.player, dropped);
            try (MockedStatic<InventoryUtils> inventories = mockStatic(InventoryUtils.class)) {
                inventories.when(() -> InventoryUtils.topInventoryForPlayer(fixture.player)).thenReturn(fixture.contents);
                fixture.listener.onPlayerDropItem(drop);
            }
            assertTrue(drop.isCancelled());

            // Paper restores a copy after the drop event. The original stack is detached.
            when(fixture.inventory.getItem(0)).thenReturn(fixture.restored);
            when(fixture.inventory.getHeldItemSlot()).thenReturn(selectedSlot);
            when(fixture.inventory.getItemInMainHand()).thenReturn(selectedSlot == 0 ? fixture.restored : null);
            ItemStack[] empty = new ItemStack[27];
            when(fixture.contents.getContents()).thenReturn(empty);
            fixture.closeBackpack();

            verify(fixture.restoredData).set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, empty);
            verify(fixture.restored).setItemMeta(fixture.restoredMeta);
            verify(fixture.inventory).setItem(0, fixture.restored);
            verify(fixture.original, never()).setItemMeta(any());
        }
    }

    @Test
    void doesNotOverwriteAnUnrelatedItemInTheOriginalSlot() {
        try (BackpackFixture fixture = new BackpackFixture()) {
            fixture.open();
            ItemStack other = mock(ItemStack.class);
            when(fixture.inventory.getItem(0)).thenReturn(other);
            fixture.closeBackpack();

            verify(other, never()).setItemMeta(any());
            verify(fixture.inventory, never()).setItem(anyInt(), any());
        }
    }

    @Test
    void savesWithdrawalsAndCancelsTheSwapBeforeTheBackpackChangesHands() {
        try (BackpackFixture fixture = new BackpackFixture();
             MockedStatic<InventoryUtils> inventories = mockStatic(InventoryUtils.class)) {
            inventories.when(() -> InventoryUtils.topInventoryForPlayer(fixture.player)).thenReturn(fixture.contents);
            fixture.open();
            when(fixture.inventory.getItem(0)).thenReturn(fixture.restored);
            ItemStack[] empty = new ItemStack[27];
            when(fixture.contents.getContents()).thenReturn(empty);
            doAnswer(invocation -> {
                fixture.closeBackpack();
                return null;
            }).when(fixture.player).closeInventory();
            PlayerSwapHandItemsEvent swap = new PlayerSwapHandItemsEvent(fixture.player, null, fixture.snapshot);

            fixture.listener.onPlayerSwapHandItems(swap);

            assertTrue(swap.isCancelled());
            verify(fixture.restoredData).set(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, empty);
            verify(fixture.player).closeInventory();
            verify(fixture.gui, never()).close(fixture.player, true);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pickupDoesNotReplaceUnrelatedStorageWhileHoldingBackpack(boolean previouslyOpenedBackpack) {
        try (BackpackFixture fixture = new BackpackFixture();
             MockedStatic<InventoryUtils> inventories = mockStatic(InventoryUtils.class)) {
            if (previouslyOpenedBackpack) fixture.open();
            Inventory unrelatedStorage = mock(Inventory.class);
            when(unrelatedStorage.getHolder()).thenReturn(mock(StorageGui.class));
            inventories.when(() -> InventoryUtils.topInventoryForPlayer(fixture.player)).thenReturn(unrelatedStorage);

            fixture.listener.onPickupItem(new EntityPickupItemEvent(fixture.player, mock(Item.class), 0));

            verify(fixture.player, never()).closeInventory();
            verify(fixture.builder, times(previouslyOpenedBackpack ? 1 : 0)).create();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pickupDoesNotRefreshClosedBackpack(boolean originalSlotMatches) {
        try (BackpackFixture fixture = new BackpackFixture();
             MockedStatic<InventoryUtils> inventories = mockStatic(InventoryUtils.class)) {
            fixture.open();
            when(fixture.original.isSimilar(fixture.snapshot)).thenReturn(true);
            if (!originalSlotMatches) when(fixture.inventory.getItem(0)).thenReturn(null);
            fixture.closeBackpack();
            inventories.when(() -> InventoryUtils.topInventoryForPlayer(fixture.player)).thenReturn(fixture.contents);

            fixture.listener.onPickupItem(new EntityPickupItemEvent(fixture.player, mock(Item.class), 0));

            verify(fixture.player, never()).closeInventory();
            verify(fixture.builder, times(1)).create();
        }
    }

    @Test
    void pickupRefreshReopensWithSavedWithdrawals() {
        try (BackpackFixture fixture = new BackpackFixture();
             MockedStatic<InventoryUtils> inventories = mockStatic(InventoryUtils.class)) {
            inventories.when(() -> InventoryUtils.topInventoryForPlayer(fixture.player)).thenReturn(fixture.contents);
            ItemMeta meta = fixture.original.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            ItemStack[] initial = {mock(ItemStack.class), mock(ItemStack.class)};
            AtomicReference<ItemStack[]> saved = new AtomicReference<>(initial);
            when(data.get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY)).thenAnswer(invocation -> saved.get().clone());
            doAnswer(invocation -> {
                saved.set(((ItemStack[]) invocation.getArgument(2)).clone());
                return null;
            }).when(data).set(eq(BACKPACK_KEY), eq(DataType.ITEM_STACK_ARRAY), any(ItemStack[].class));
            when(fixture.original.isSimilar(fixture.snapshot)).thenReturn(true);
            fixture.open();
            verify(fixture.contents).setContents(initial);

            ItemStack[] withdrawn = new ItemStack[27];
            withdrawn[1] = initial[1];
            when(fixture.contents.getContents()).thenReturn(withdrawn);
            doAnswer(invocation -> {
                fixture.closeBackpack();
                return null;
            }).when(fixture.player).closeInventory();

            StorageGui reopened = mock(StorageGui.class);
            Inventory reopenedContents = mock(Inventory.class);
            when(reopened.getInventory()).thenReturn(reopenedContents);
            when(fixture.builder.create()).thenReturn(reopened);
            doAnswer(invocation -> {
                fixture.executeOpen(reopened);
                return null;
            }).when(reopened).open(fixture.player);
            EntityPickupItemEvent pickup = new EntityPickupItemEvent(fixture.player, mock(Item.class), 0);

            fixture.listener.onPickupItem(pickup);

            verify(fixture.player).closeInventory();
            verify(fixture.original).setItemMeta(meta);
            verify(fixture.inventory).setItem(0, fixture.original);
            verify(reopened).open(fixture.player);
            ArgumentCaptor<ItemStack[]> loaded = ArgumentCaptor.forClass(ItemStack[].class);
            verify(reopenedContents).setContents(loaded.capture());
            assertArrayEquals(withdrawn, saved.get());
            assertArrayEquals(withdrawn, loaded.getValue());
        }
    }

    private static class BackpackFixture implements AutoCloseable {
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        final ItemStack original = mock(ItemStack.class);
        final ItemStack snapshot = mock(ItemStack.class);
        final ItemStack restored = mock(ItemStack.class);
        final ItemMeta restoredMeta = mock(ItemMeta.class);
        final PersistentDataContainer restoredData = mock(PersistentDataContainer.class);
        final Inventory contents = mock(Inventory.class);
        final StorageGui gui = mock(StorageGui.class);
        final StorageBuilder builder = mock(StorageBuilder.class, RETURNS_SELF);
        final BackpackListener listener;
        final MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class);
        final MockedStatic<Gui> guis = mockStatic(Gui.class);

        BackpackFixture() {
            BackpackMechanicFactory factory = mock(BackpackMechanicFactory.class);
            BackpackMechanic mechanic = mock(BackpackMechanic.class);
            listener = new BackpackListener(factory);
            items.when(() -> OraxenItems.getIdByItem(original)).thenReturn("backpack");
            items.when(() -> OraxenItems.getIdByItem(snapshot)).thenReturn("backpack");
            when(factory.getMechanic("backpack")).thenReturn(mechanic);
            when(mechanic.getTitle()).thenReturn("Backpack");
            when(mechanic.getRows()).thenReturn(3);
            when(player.getInventory()).thenReturn(inventory);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(inventory.getItemInMainHand()).thenReturn(original);
            when(inventory.getItem(0)).thenReturn(original);
            when(original.clone()).thenReturn(snapshot);
            ItemMeta meta = mock(ItemMeta.class);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(original.getItemMeta()).thenReturn(meta);
            when(snapshot.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(data);
            when(data.get(BACKPACK_KEY, DataType.ITEM_STACK_ARRAY)).thenReturn(new ItemStack[]{mock(ItemStack.class)});
            when(restored.isSimilar(snapshot)).thenReturn(true);
            when(restored.getItemMeta()).thenReturn(restoredMeta);
            when(restoredMeta.getPersistentDataContainer()).thenReturn(restoredData);
            guis.when(Gui::storage).thenReturn(builder);
            when(builder.create()).thenReturn(gui);
            when(gui.getInventory()).thenReturn(contents);
            when(contents.getHolder()).thenReturn(gui);
        }

        @SuppressWarnings("unchecked")
        void open() {
            PlayerInteractEvent event = mock(PlayerInteractEvent.class);
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            listener.onPlayerInteract(event);
            executeOpen(gui);
        }

        @SuppressWarnings("unchecked")
        void executeOpen(StorageGui openedGui) {
            ArgumentCaptor<GuiAction<InventoryOpenEvent>> action = ArgumentCaptor.forClass(GuiAction.class);
            verify(openedGui).setOpenGuiAction(action.capture());
            InventoryOpenEvent open = mock(InventoryOpenEvent.class);
            when(open.getPlayer()).thenReturn(player);
            action.getValue().execute(open);
        }

        @SuppressWarnings("unchecked")
        void closeBackpack() {
            ArgumentCaptor<GuiAction<InventoryCloseEvent>> action = ArgumentCaptor.forClass(GuiAction.class);
            verify(gui).setCloseGuiAction(action.capture());
            InventoryCloseEvent close = mock(InventoryCloseEvent.class);
            when(close.getPlayer()).thenReturn(player);
            action.getValue().execute(close);
        }

        @Override
        public void close() {
            guis.close();
            items.close();
        }
    }
}
