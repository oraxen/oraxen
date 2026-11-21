package io.th0rgal.oraxen.mechanics;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerCompatibility;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import java.lang.reflect.Method;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FurnitureGhastLifecycleTest extends MechanicTestSupport {

    @Test
    void activatesStartupHitboxesOnceAndUntracksOnUnload() throws Exception {
        ConfigsManager configs = mock(ConfigsManager.class);
        when(OraxenPlugin.get().getConfigsManager()).thenReturn(configs);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("debug", false);
        when(configs.getSettings()).thenReturn(settings);

        Class<?> type = Class.forName("io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureGhastEntities");
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object listener = constructor.newInstance();
        Method itemsLoaded = type.getDeclaredMethod("onItemsLoaded", OraxenItemsLoadedEvent.class);
        Method removed = type.getDeclaredMethod("onEntityRemove", EntityRemoveEvent.class);
        itemsLoaded.setAccessible(true);
        removed.setAccessible(true);

        HappyGhast ghast = mock(HappyGhast.class);
        when(ghast.getEntityId()).thenReturn(42);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(ghast.getPersistentDataContainer()).thenReturn(data);
        when(data.has(any())).thenReturn(true);
        World world = mock(World.class);
        when(world.getEntitiesByClass(HappyGhast.class)).thenReturn(List.of(ghast));
        NMSHandler handler = mock(NMSHandler.class);
        when(handler.keepGhastStationary(ghast)).thenReturn(true);

        try (var bukkit = mockStatic(Bukkit.class);
             var handlers = mockStatic(NMSHandlers.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            handlers.when(NMSHandlers::getHandler).thenReturn(handler);
            scheduler.when(() -> SchedulerUtil.runOnOwningThread(eq(ghast), any(Runnable.class)))
                    .thenAnswer(call -> { call.<Runnable>getArgument(1).run(); return null; });
            scheduler.when(() -> SchedulerUtil.runForEntityTimer(eq(ghast), eq(5L), eq(5L), any(), any()))
                    .thenReturn(mock(SchedulerUtil.ScheduledTask.class));

            itemsLoaded.invoke(listener, new OraxenItemsLoadedEvent());
            itemsLoaded.invoke(listener, new OraxenItemsLoadedEvent());
            verify(handler, times(2)).trackFurnitureHitbox(ghast);
            scheduler.verify(() -> SchedulerUtil.runForEntityTimer(eq(ghast), eq(5L), eq(5L), any(), any()), times(1));

            EntityRemoveEvent unload = mock(EntityRemoveEvent.class);
            when(unload.getEntity()).thenReturn(ghast);
            when(unload.getCause()).thenReturn(EntityRemoveEvent.Cause.UNLOAD);
            removed.invoke(listener, unload);
            verify(handler).untrackFurnitureHitbox(ghast);
            verify(data, never()).remove(any());

            Method activate = type.getDeclaredMethod("activate", Entity.class);
            activate.setAccessible(true);
            assertTrue((boolean) activate.invoke(null, ghast));
            scheduler.verify(() -> SchedulerUtil.runForEntityTimer(eq(ghast), eq(5L), eq(5L), any(), any()), times(2));
            removed.invoke(listener, unload);
        }
    }

    @Test
    void deniedLockStopsAttackBeforeBreakChecks() throws Exception {
        var listener = mock(FurnitureListener.class, CALLS_REAL_METHODS);
        var event = mock(PrePlayerAttackEntityEvent.class);
        var player = mock(Player.class);
        Entity ghast = mock(Entity.class);
        Entity base = mock(Entity.class);
        var location = mock(Location.class);
        var block = mock(Block.class);
        var mechanic = mock(FurnitureMechanic.class);
        when(event.getPlayer()).thenReturn(player);
        when(ghast.getLocation()).thenReturn(location);
        when(location.getBlock()).thenReturn(block);
        when(mechanic.getBaseEntity(ghast)).thenReturn(base);
        try (var furniture = mockStatic(OraxenFurniture.class);
             var bukkit = mockStatic(Bukkit.class);
             var locks = mockStatic(BlockLockerCompatibility.class);
             var protection = mockStatic(AntiGriefLib.class)) {
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(ghast)).thenReturn(mechanic);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(base)).thenReturn(true);
            protection.when(() -> AntiGriefLib.canInteract(player, location)).thenReturn(true);
            protection.when(() -> AntiGriefLib.canBreak(eq(player), any())).thenReturn(true);
            locks.when(() -> BlockLockerCompatibility.canInteract(player, block, mechanic)).thenReturn(false);
            Method attack = FurnitureListener.class.getDeclaredMethod("onSolidHitboxAttack",
                    PrePlayerAttackEntityEvent.class, Entity.class);
            attack.setAccessible(true);
            attack.invoke(listener, event, ghast);
            verify(event).setCancelled(true);
            verify(mechanic, never()).runEvents(any(), any());
            verify(mechanic, never()).isBreakable();
            furniture.verify(() -> OraxenFurniture.remove(eq(base), eq(player), any()), never());
        }
    }
}
