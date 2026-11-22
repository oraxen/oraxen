package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemProperties;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.ArmorStandProperties;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurniturePacketDispatcher;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import io.th0rgal.oraxen.utils.logs.Logs;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FurnitureMechanicTest extends MechanicTestSupport {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sitActionsRunOnlyAfterSuccessfulMount(boolean accepted) {
        ConfigurationSection section = mechanicSection("furniture", "events", List.of(Map.of(
                "trigger", "sit", "actions", List.of(Map.of("command", "say seated <player>")))));
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("testPlayer");
        assertFalse(mechanic.runEvents(player, Action.RIGHT_CLICK_BLOCK));
        assertFalse(mechanic.runEvents(player, Action.LEFT_CLICK_BLOCK));
        mechanic.runBreakEvents(player);
        mechanic.runPlaceEvents(player);
        verify(player, never()).performCommand(any());

        Entity seat = mock(Entity.class);
        UUID seatId = UUID.randomUUID();
        World world = mock(World.class);
        when(seat.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(seat.addPassenger(player)).thenReturn(accepted);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        doReturn(List.of(seatId)).when(pdc).getOrDefault(eq(FurnitureMechanic.SEATS_KEY), any(), any());
        when(pdc.get(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING)).thenReturn("chair");

        try (var bukkit = mockStatic(Bukkit.class);
             var furniture = mockStatic(OraxenFurniture.class);
             var packets = mockStatic(FurniturePacketDispatcher.class);
             var scheduler = mockStatic(SchedulerUtil.class);
             var compatibilities = mockStatic(CompatibilitiesManager.class)) {
            bukkit.when(() -> Bukkit.getEntity(seatId)).thenReturn(seat);
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic("chair")).thenReturn(mechanic);
            ArgumentCaptor<Runnable> mount = ArgumentCaptor.forClass(Runnable.class);
            ArgumentCaptor<Runnable> actions = ArgumentCaptor.forClass(Runnable.class);

            FurnitureMechanic.sitOnSeat(pdc, player);
            scheduler.verify(() -> SchedulerUtil.runForEntity(eq(seat), mount.capture(), isNull()));
            verify(player, never()).performCommand(any());
            mount.getValue().run();
            verify(seat).addPassenger(player);
            if (accepted) {
                scheduler.verify(() -> SchedulerUtil.runForEntity(eq(player), actions.capture(), isNull()));
                actions.getValue().run();
                verify(player).performCommand("say seated testPlayer");
            } else {
                scheduler.verify(() -> SchedulerUtil.runForEntity(eq(player), any(), isNull()), never());
                verify(player, never()).performCommand(any());
            }
        }
    }

    @Test
    void occupiedSeatDoesNotRunSitActions() {
        Entity seat = mock(Entity.class);
        Player player = mock(Player.class);
        when(seat.getPassengers()).thenReturn(List.of(mock(Entity.class)));
        UUID seatId = UUID.randomUUID();
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        doReturn(List.of(seatId)).when(pdc).getOrDefault(eq(FurnitureMechanic.SEATS_KEY), any(), any());

        try (var bukkit = mockStatic(Bukkit.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            bukkit.when(() -> Bukkit.getEntity(seatId)).thenReturn(seat);
            FurnitureMechanic.sitOnSeat(pdc, player);
            verify(seat, never()).addPassenger(any());
            scheduler.verifyNoInteractions();
        }
    }

    @Test
    void breakActionsKeepFurnitureBreakableAndDoNotRunOnClicks() {
        ConfigurationSection section = mechanicSection("furniture", "events", List.of(Map.of(
                "trigger", "break", "actions", List.of(Map.of("command", "say broken")))));
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);
        Player player = mock(Player.class);

        when(player.getName()).thenReturn("testPlayer");
        assertTrue(mechanic.isBreakable());
        assertFalse(mechanic.runEvents(player, Action.LEFT_CLICK_BLOCK));
        assertFalse(mechanic.runEvents(player, Action.RIGHT_CLICK_BLOCK));
        try (var compatibilities = mockStatic(CompatibilitiesManager.class)) {
            mechanic.runBreakEvents(player);
            verify(player).performCommand("say broken");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void removalOfMultiBarrierFurnitureRunsBreakActionsOnceOnlyWithPlayer(boolean hasPlayer) {
        ConfigurationSection section = mechanicSection("furniture", "barriers", List.of("0,0,0", "1,0,0"));
        section.set("events", List.of(Map.of("trigger", "break", "actions", List.of(Map.of("command", "say broken")))));
        FurnitureMechanic mechanic = spy(new FurnitureMechanic(mechanicFactory(), section));
        ItemDisplay entity = mock(ItemDisplay.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 64, 0);
        Player player = hasPlayer ? mock(Player.class) : null;
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        doNothing().when(mechanic).removeSolid(entity, location, 0f);
        if (hasPlayer) {
            when(player.getName()).thenReturn("testPlayer");
            when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
            when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        }

        try (var furniture = mockStatic(OraxenFurniture.class, CALLS_REAL_METHODS);
                var factory = mockStatic(FurnitureFactory.class);
                var furnitureStatics = mockStatic(FurnitureMechanic.class);
                var compatibilities = mockStatic(CompatibilitiesManager.class)) {
            factory.when(FurnitureFactory::isEnabled).thenReturn(true);
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(entity)).thenReturn(mechanic);
            furniture.when(() -> OraxenFurniture.isInteractionEntity(entity)).thenReturn(false);

            assertEquals(2, mechanic.getBarriers().size());
            assertTrue(OraxenFurniture.remove(entity, player, null));

            verify(mechanic).removeSolid(entity, location, 0f);
            if (hasPlayer) {
                var order = inOrder(mechanic, player);
                order.verify(mechanic).removeSolid(entity, location, 0f);
                order.verify(player).performCommand("say broken");
                verify(player, times(1)).performCommand("say broken");
            } else {
                verify(mechanic, never()).runBreakEvents(any());
            }
        }
    }

    @Test
    void readsBarrierOffsetsAndDisablesDefaultInteractionHitbox() {
        ConfigurationSection section = mechanicSection("furniture", "barriers",
                List.of("0,0,0", " -2, 1, 3 "));
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);

        assertEquals(List.of("0,0,0", "-2,1,3"), mechanic.getBarriers().stream().map(Object::toString).toList());
        assertTrue(mechanic.hasBarriers());
        assertFalse(mechanic.hasHitbox());
    }

    @ParameterizedTest
    @ValueSource(strings = {"origin", "1,2", "1,2,3,4", "1.5,0,0", "1,2,", "2147483648,0,0"})
    void skipsInvalidBarrierOffsetsWithoutPreventingValidEntries(String invalidOffset) {
        ConfigurationSection section = mechanicSection("furniture", "barriers",
                List.of(invalidOffset, "-1,0,2"));
        try (var logs = mockStatic(Logs.class)) {
            FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), section);

            assertEquals(List.of("-1,0,2"), mechanic.getBarriers().stream().map(Object::toString).toList());
            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
            logs.verify(() -> Logs.logError(message.capture()));
            assertTrue(message.getValue().contains(invalidOffset));
            assertTrue(message.getValue().contains(mechanic.getItemID()));
        }
    }

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
