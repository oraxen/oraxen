package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

class FurnitureBedReloadTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unregisterCleansUpAnOccupiedBedEvenIfThePlayerDisconnects(boolean disconnected) {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.namespace()).thenReturn("oraxen");
        when(plugin.isEnabled()).thenReturn(true);

        World world = mock(World.class);
        Location location = new Location(world, 0.5, 64, 0.5);
        Entity base = mock(Entity.class);
        when(base.getLocation()).thenReturn(location);
        when(base.getUniqueId()).thenReturn(UUID.randomUUID());

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getEntityId()).thenReturn(42);
        EntityScheduler playerScheduler = mock(EntityScheduler.class);
        ScheduledTask timer = mock(ScheduledTask.class);
        when(player.getScheduler()).thenReturn(playerScheduler);
        when(playerScheduler.runAtFixedRate(same(plugin), any(), any(), eq(20L), eq(20L))).thenReturn(timer);
        when(playerScheduler.run(same(plugin), any(), any())).thenReturn(mock(ScheduledTask.class));

        ItemDisplay carrier = mock(ItemDisplay.class);
        EntityScheduler carrierScheduler = mock(EntityScheduler.class);
        when(carrier.getScheduler()).thenReturn(carrierScheduler);
        when(carrier.addPassenger(player)).thenReturn(true);
        when(player.getVehicle()).thenReturn(carrier);
        when(world.spawn(any(Location.class), eq(ItemDisplay.class),
                ArgumentMatchers.<Consumer<? super ItemDisplay>>any())).thenReturn(carrier);
        NMSHandler packets = mock(NMSHandler.class);
        when(packets.showFurnitureBed(eq(player), any(Location.class), eq(0f))).thenReturn(true);

        try (var oraxen = mockStatic(OraxenPlugin.class);
             var bukkit = mockStatic(Bukkit.class);
             var handlers = mockStatic(NMSHandlers.class)) {
            oraxen.when(OraxenPlugin::get).thenReturn(plugin);
            bukkit.when(Bukkit::getMinecraftVersion).thenReturn("1.21.11");
            bukkit.when(() -> Bukkit.getTag(anyString(), any(NamespacedKey.class), any()))
                    .thenAnswer(ignored -> mock(Tag.class));
            bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(Location.class))).thenReturn(true);
            handlers.when(NMSHandlers::getHandler).thenReturn(packets);
            FurnitureFactory factory = mock(FurnitureFactory.class, CALLS_REAL_METHODS);
            FurnitureMechanic mechanic = mock(FurnitureMechanic.class);
            when(mechanic.getFactory()).thenReturn(factory);
            when(mechanic.getBeds()).thenReturn(List.of(FurnitureBed.parse("0,0.6875,0 true false false")));
            MechanicsManager.registerMechanicFactory("furniture", factory, true);
            try {
                FurnitureBeds.lieDown(player, base, mechanic, location);
                verify(player).setSleepingIgnored(true);
            } finally {
                MechanicsManager.unregisterMechanicFactory("furniture");
            }
            try {
                var wake = ArgumentCaptor.<Consumer<ScheduledTask>>captor();
                var retired = ArgumentCaptor.forClass(Runnable.class);
                verify(playerScheduler).run(same(plugin), wake.capture(), retired.capture());
                if (disconnected) {
                    retired.getValue().run();
                    verify(packets).forgetFurnitureBed(42);
                } else {
                    wake.getValue().accept(null);
                    verify(packets).hideFurnitureBed(player);
                    verify(player).setSleepingIgnored(false);
                    verify(player).leaveVehicle();
                }

                verify(timer).cancel();
                var removeCarrier = ArgumentCaptor.<Consumer<ScheduledTask>>captor();
                verify(carrierScheduler).run(same(plugin), removeCarrier.capture(), isNull());
                verify(carrier, never()).remove();
                removeCarrier.getValue().accept(null);
                verify(carrier).remove();
            } finally {
                FurnitureBeds.leave(player);
            }
        }
    }
}
