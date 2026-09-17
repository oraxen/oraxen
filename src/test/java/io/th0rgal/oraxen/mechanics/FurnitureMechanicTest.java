package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FurnitureMechanicTest extends MechanicTestSupport {

    @Test
    void readsBasicFurnitureSettings() {
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), mechanicSection("furniture",
                "hardness", 4,
                "item", "placed_item",
                "type", "ITEM_FRAME",
                "seat", java.util.Map.of("height", 0.75, "yaw", 90.0),
                "rotatable", true));

        assertEquals(4, mechanic.getHardness());
        assertTrue(mechanic.hasHardness());
        assertEquals(FurnitureMechanic.FurnitureType.ITEM_FRAME, mechanic.getFurnitureType());
        assertTrue(mechanic.hasSeat());
        assertEquals(0.75f, mechanic.getSeatHeight());
        assertTrue(mechanic.hasHitbox());
        assertFalse(mechanic.hasLimitedPlacing());
        assertFalse(mechanic.hasBlockSounds());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void droppedFurnitureClearsPlacementNameForItemNameOnlyItems(boolean modernNameApi) {
        assertFurnitureDropName(modernNameApi, null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void droppedFurnitureRestoresConfiguredDisplayName(boolean modernNameApi) {
        assertFurnitureDropName(modernNameApi, Component.text("Furniture display name"));
    }

    private void assertFurnitureDropName(boolean modernNameApi, Component configuredName) {
        String itemId = "test_furniture";
        ItemBuilder builder = mock(ItemBuilder.class);
        ItemStack baseItem = mock(ItemStack.class);
        ItemMeta baseMeta = mock(ItemMeta.class);
        ItemStack placedItem = mock(ItemStack.class);
        LeatherArmorMeta placedMeta = mock(LeatherArmorMeta.class);
        ItemStack droppedItem = mock(ItemStack.class);
        ItemDisplay furniture = mock(ItemDisplay.class);
        Location location = mock(Location.class);
        World world = mock(World.class);

        when(builder.build()).thenReturn(baseItem);
        when(baseItem.getItemMeta()).thenReturn(baseMeta);
        when(baseMeta.hasItemName()).thenReturn(true);
        when(baseMeta.itemName()).thenReturn(Component.text("Furniture item name"));
        when(baseMeta.hasCustomName()).thenReturn(configuredName != null);
        when(baseMeta.customName()).thenReturn(configuredName);
        when(baseMeta.hasDisplayName()).thenReturn(configuredName != null);
        when(baseMeta.displayName()).thenReturn(configuredName);
        when(placedItem.getItemMeta()).thenReturn(placedMeta);
        when(placedItem.clone()).thenReturn(droppedItem);
        when(droppedItem.getAmount()).thenReturn(1);
        when(droppedItem.getMaxStackSize()).thenReturn(64);
        when(furniture.getType()).thenReturn(EntityType.ITEM_DISPLAY);
        when(furniture.getItemStack()).thenReturn(placedItem);
        when(furniture.getLocation()).thenReturn(location);
        when(location.toBlockLocation()).thenReturn(location);
        when(location.isWorldLoaded()).thenReturn(true);
        when(location.getWorld()).thenReturn(world);

        try (var versions = mockStatic(VersionUtil.class);
             var items = mockStatic(OraxenItems.class);
             var updater = mockStatic(ItemUpdater.class)) {
            versions.when(() -> VersionUtil.atOrAbove("1.21.4")).thenReturn(modernNameApi);
            items.when(() -> OraxenItems.getItemById(itemId)).thenReturn(builder);
            items.when(() -> OraxenItems.getIdByItem(baseItem)).thenReturn(itemId);
            items.when(() -> OraxenItems.getIdByItem(placedItem)).thenReturn(itemId);
            updater.when(() -> ItemUpdater.updateItem(any(ItemStack.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Drop drop = new Drop(List.of(new Loot(baseItem, 1.0)), false, false, itemId);
            drop.furnitureSpawns(furniture, null);

            if (modernNameApi) verify(placedMeta).customName(configuredName);
            else verify(placedMeta).displayName(configuredName);
            verify(placedItem).setItemMeta(placedMeta);
            verify(placedMeta, never()).itemName(any());
            verify(placedMeta, never()).setColor(any());
            verify(world).dropItemNaturally(location, droppedItem);
        }
    }
}
