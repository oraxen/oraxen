package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.ConfigsManager;
import io.th0rgal.oraxen.configs.Settings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PackLoadingManagerTest {
    private final PackLoadingManager manager = new PackLoadingManager();
    private final Player player = mock(Player.class);
    private final AtomicBoolean allowFlight = new AtomicBoolean();
    private final YamlConfiguration settings = new YamlConfiguration();
    private MockedStatic<OraxenPlugin> pluginAccess;

    @BeforeEach
    void setup() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        ConfigsManager configs = mock(ConfigsManager.class);
        when(plugin.getConfigsManager()).thenReturn(configs);
        when(configs.getSettings()).thenReturn(settings);
        pluginAccess = mockStatic(OraxenPlugin.class);
        pluginAccess.when(OraxenPlugin::get).thenReturn(plugin);
        settings.set(Settings.DISABLE_MOVEMENT_ON_LOAD.getPath(), true);
        settings.set(Settings.DISABLE_DAMAGE_ON_LOAD.getPath(), true);
        Settings.invalidateCache();
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getAllowFlight()).thenAnswer(invocation -> allowFlight.get());
        doAnswer(invocation -> {
            allowFlight.set(invocation.getArgument(0));
            return null;
        }).when(player).setAllowFlight(anyBoolean());
    }

    @AfterEach
    void cleanup() {
        Settings.invalidateCache();
        pluginAccess.close();
    }

    @Test
    void grantsPermissionWithoutEnablingFlightAndStillFreezesAirbornePlayer() {
        status(Status.ACCEPTED);
        assertTrue(allowFlight.get());
        verify(player, never()).setFlying(true);
        PlayerMoveEvent move = move();
        manager.onPlayerMove(move);
        assertTrue(move.isCancelled());
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        manager.onEntityDamage(damage);
        verify(damage).setCancelled(true);
        PlayerToggleFlightEvent toggle = new PlayerToggleFlightEvent(player, true);
        manager.onPlayerToggleFlight(toggle);
        assertTrue(toggle.isCancelled());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"ACCEPTED", "DOWNLOADED"}, mode = EnumSource.Mode.EXCLUDE)
    void everyTerminalStatusRestoresFlightAndMovement(Status terminalStatus) {
        status(Status.ACCEPTED);
        status(terminalStatus);
        assertFalse(allowFlight.get());
        PlayerMoveEvent move = move();
        manager.onPlayerMove(move);
        assertFalse(move.isCancelled());
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        manager.onEntityDamage(damage);
        verify(damage, never()).setCancelled(true);
    }

    @Test
    void repeatedAcceptanceAndDownloadedStatusDoNotLoseOriginalPermission() {
        status(Status.ACCEPTED);
        status(Status.ACCEPTED);
        status(Status.DOWNLOADED);
        assertTrue(allowFlight.get());
        status(Status.SUCCESSFULLY_LOADED);
        assertFalse(allowFlight.get());
    }

    @Test
    void preservesExistingFlightPermissionAndFlyingState() {
        allowFlight.set(true);
        when(player.isFlying()).thenReturn(true);
        status(Status.ACCEPTED);
        status(Status.SUCCESSFULLY_LOADED);
        assertTrue(allowFlight.get());
        verify(player, never()).setFlying(anyBoolean());
        verify(player, never()).setAllowFlight(anyBoolean());
    }

    @Test
    void damageOnlyProtectionDoesNotGrantFlightOrFreezeMovement() {
        settings.set(Settings.DISABLE_MOVEMENT_ON_LOAD.getPath(), false);
        status(Status.ACCEPTED);
        assertFalse(allowFlight.get());
        PlayerMoveEvent move = move();
        manager.onPlayerMove(move);
        assertFalse(move.isCancelled());
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        when(damage.getEntity()).thenReturn(player);
        manager.onEntityDamage(damage);
        verify(damage).setCancelled(true);
    }

    @Test
    void disablingMovementFreezeDuringLoadingRestoresPermission() {
        status(Status.ACCEPTED);
        settings.set(Settings.DISABLE_MOVEMENT_ON_LOAD.getPath(), false);
        Settings.invalidateCache();
        manager.onPlayerMove(move());
        assertFalse(allowFlight.get());
    }

    @Test
    void quittingRestoresPermissionBeforePlayerDataIsSaved() {
        status(Status.ACCEPTED);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(player);
        manager.onPlayerQuit(quit);
        assertFalse(allowFlight.get());
    }

    @Test
    void shutdownRestoresTemporaryPermissionAndStopsFlying() {
        status(Status.ACCEPTED);
        when(player.isFlying()).thenReturn(true);
        manager.shutdown();
        assertFalse(allowFlight.get());
        verify(player).setFlying(false);
        manager.shutdown();
        verify(player, times(1)).setAllowFlight(false);
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void doesNotRevokeFlightFromNewGameMode(GameMode gameMode) {
        status(Status.ACCEPTED);
        PlayerGameModeChangeEvent change = new PlayerGameModeChangeEvent(player, gameMode, PlayerGameModeChangeEvent.Cause.PLUGIN, null);
        manager.onPlayerGameModeChange(change);
        when(player.getGameMode()).thenReturn(gameMode);
        allowFlight.set(true);
        status(Status.SUCCESSFULLY_LOADED);
        assertTrue(allowFlight.get());
    }

    @Test
    void reappliesProtectionAfterGameModeResetsFlightPermission() {
        status(Status.ACCEPTED);
        manager.onPlayerGameModeChange(new PlayerGameModeChangeEvent(player, GameMode.ADVENTURE, PlayerGameModeChangeEvent.Cause.PLUGIN, null));
        when(player.getGameMode()).thenReturn(GameMode.ADVENTURE);
        allowFlight.set(false);
        manager.onPlayerMove(move());
        assertTrue(allowFlight.get());
        status(Status.SUCCESSFULLY_LOADED);
        assertFalse(allowFlight.get());
    }

    private PlayerMoveEvent move() {
        return new PlayerMoveEvent(player, new Location(null, 0, 100, 0), new Location(null, 0, 99, 0));
    }

    private void status(Status status) {
        PlayerResourcePackStatusEvent event = mock(PlayerResourcePackStatusEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getStatus()).thenReturn(status);
        manager.onPlayerUpdatesPackStatus(event);
    }
}
