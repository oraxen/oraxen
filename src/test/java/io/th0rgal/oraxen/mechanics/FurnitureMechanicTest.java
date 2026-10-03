package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.items.ItemProperties;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.ArmorStandProperties;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

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
    void readsUnifiedDisplayPropertiesAndIgnoresArmorStandOffset() {
        ConfigurationSection section = mechanicSection("furniture", "type", "DISPLAY_ENTITY");
        section.set("properties.display_transform", "FIXED");
        section.set("properties.scale.y", 2.0);
        section.set("properties.translation.x", 0.25);
        section.set("properties.offset.y", 10.0);
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);

        assertEquals(ItemDisplay.ItemDisplayTransform.FIXED, mechanic.getDisplayEntityProperties().getDisplayTransform());
        assertEquals(2.0f, mechanic.getDisplayEntityProperties().getScale().y());
        assertEquals(0.25f, mechanic.getDisplayEntityProperties().getTranslation().x());
        assertEquals(0.0f, mechanic.getDisplayEntityProperties().getTranslation().y());
    }

    @Test
    void readsUnifiedArmorStandPropertiesAndModelScaleWhileIgnoringDisplayProperties() throws Exception {
        ConfigurationSection section = mechanicSection("furniture", "type", "ARMOR_STAND");
        section.set("properties.scale.y", 0.5);
        section.set("properties.translation.x", 0.25);
        // These values would fail display property parsing if it ran for an armor stand.
        section.set("properties.display_transform", "INVALID");
        section.set("properties.brightness.block_light", 100);
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);

        var armorStandField = FurnitureMechanic.class.getDeclaredField("armorStandProperties");
        armorStandField.setAccessible(true);
        ArmorStandProperties armorStandProperties = (ArmorStandProperties) armorStandField.get(mechanic);
        assertEquals(0.25, armorStandProperties.getTranslation().getX());
        assertEquals(0.5f, armorStandProperties.getScaleY());
        var smallField = FurnitureMechanic.class.getDeclaredField("small");
        smallField.setAccessible(true);
        assertTrue(smallField.getBoolean(mechanic));

        OraxenMeta meta = new OraxenMeta();
        ItemProperties properties = new ItemProperties(section.getParent().getParent(), Material.PAPER, meta, Map.of());
        var applyModelProperties = ItemProperties.class.getDeclaredMethod("applyArmorStandModelProperties", ConfigurationSection.class);
        applyModelProperties.setAccessible(true);
        applyModelProperties.invoke(properties, section.getParent().getParent());
        assertEquals(0.5, meta.getArmorStandHeadScale().getY());
        assertEquals(ItemDisplay.ItemDisplayTransform.NONE, mechanic.getDisplayEntityProperties().getDisplayTransform());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ITEM_FRAME", "GLOW_ITEM_FRAME"})
    void ignoresDisplayPropertiesForItemFrames(String type) {
        ConfigurationSection section = mechanicSection("furniture", "type", type);
        section.set("properties.display_transform", "INVALID");
        section.set("properties.brightness.block_light", 100);

        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);

        assertEquals(ItemDisplay.ItemDisplayTransform.NONE, mechanic.getDisplayEntityProperties().getDisplayTransform());
        assertFalse(mechanic.getDisplayEntityProperties().hasBrightness());
    }

    @Test
    void runsConfiguredFurnitureEventsForMatchingClick() {
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), mechanicSection("furniture",
                "events", List.of(Map.of("click", "right", "actions", List.of(
                        Map.of("command", "say <Player>", "executor", "PLAYER"))))));
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");

        try (var compatibilities = mockStatic(CompatibilitiesManager.class)) {
            assertFalse(mechanic.runEvents(player, Action.LEFT_CLICK_BLOCK));
            assertTrue(mechanic.runEvents(player, Action.RIGHT_CLICK_BLOCK));
        }
        verify(player).performCommand("say Alex");
    }

    @Test
    void readsBasicFurnitureSettings() {
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), mechanicSection("furniture",
                "hardness", 4,
                "item", "placed_item",
                "type", "ITEM_FRAME",
                "seats", List.of("0,-0.25,0 90"),
                "rotatable", true));

        assertEquals(4, mechanic.getHardness());
        assertTrue(mechanic.hasHardness());
        assertEquals(FurnitureMechanic.FurnitureType.ITEM_FRAME, mechanic.getFurnitureType());
        assertTrue(mechanic.hasSeat());
        assertEquals(-0.25, mechanic.getSeats().getFirst().offsetY());
        assertEquals(90.0f, mechanic.getSeats().getFirst().yaw());
        assertTrue(mechanic.hasHitbox());
        assertFalse(mechanic.hasLimitedPlacing());
        assertFalse(mechanic.hasBlockSounds());
        assertTrue(mechanic.isBreakable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"left", "both"})
    void leftClickEventsMakeFurnitureUnbreakableByDefault(String click) {
        List<Map<String, Object>> events = List.of(Map.of("click", click,
                "actions", List.of(Map.of("message", "clicked"))));
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(),
                mechanicSection("furniture", "events", events));
        FurnitureMechanic explicitlyBreakable = new FurnitureMechanic(mechanicFactory(),
                mechanicSection("furniture", "events", events, "breakable", true));

        assertFalse(mechanic.isBreakable());
        assertTrue(explicitlyBreakable.isBreakable());
    }

    @Test
    void rightClickEventsKeepFurnitureBreakableUnlessDisabled() {
        List<Map<String, Object>> events = List.of(Map.of("click", "right",
                "actions", List.of(Map.of("message", "clicked"))));
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(),
                mechanicSection("furniture", "events", events));
        FurnitureMechanic explicitlyUnbreakable = new FurnitureMechanic(mechanicFactory(),
                mechanicSection("furniture", "events", events, "breakable", false));

        assertTrue(mechanic.isBreakable());
        assertFalse(explicitlyUnbreakable.isBreakable());
    }

    @Test
    void eventWithoutClickFilterAlsoMakesFurnitureUnbreakable() {
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), mechanicSection("furniture",
                "events", List.of(Map.of("actions", List.of(Map.of("message", "clicked"))))));

        assertFalse(mechanic.isBreakable());
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
