package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.breaker.BreakSpeedModifier;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adjusts mining speed while a player digs with an item carrying the efficiency mechanic.
 *
 * <p>On 1.20.5+ this is a transient {@code BLOCK_BREAK_SPEED} modifier; older servers fall back to
 * the haste/mining-fatigue potion effects the mechanic modelled its {@code amount} on.</p>
 */
public class EfficiencyMechanicListener implements Listener {

    private static final int FALLBACK_EFFECT_DURATION = 20 * 60 * 5;
    // Each listener instance gets its own modifier key: on reload the replacement listener
    // starts applying modifiers while the old listener's async clearAll may still be pending,
    // and sharing a key would either throw on the duplicate add or let the old clear strip
    // the new listener's modifier.
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

    private final EfficiencyMechanicFactory factory;
    private final BreakSpeedModifier breakSpeedModifier = new BreakSpeedModifier(
            NamespacedKey.fromString("oraxen:efficiency_mining_speed_" + INSTANCE_COUNTER.incrementAndGet()));
    private final Map<UUID, FallbackEffect> fallbackEffects = new ConcurrentHashMap<>();

    public EfficiencyMechanicListener(final EfficiencyMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartDigging(final BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            clear(player);
            return;
        }

        final EfficiencyMechanic mechanic = getMechanic(player.getInventory().getItemInMainHand());
        if (mechanic == null) {
            clear(player);
            return;
        }

        if (BreakSpeedModifier.isSupported()) breakSpeedModifier.apply(player, mechanic.getMiningSpeedMultiplier());
        else applyFallbackEffect(player, mechanic);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAbortDigging(final BlockDamageAbortEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(final BlockBreakEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeHeldItem(final PlayerItemHeldEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHand(final PlayerSwapHandItemsEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDropHand(final PlayerDropItemEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisconnect(final PlayerQuitEvent event) {
        clear(event.getPlayer());
    }

    /**
     * Drops every bit of mining-speed state this listener applied. Called when the mechanic is
     * unregistered (reload) so the discarded listener does not strand a modifier or fallback effect
     * on players, which the replacement listener would have no handle on.
     */
    public void clearAll() {
        breakSpeedModifier.clearAll();
        for (final UUID uuid : List.copyOf(fallbackEffects.keySet())) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                fallbackEffects.remove(uuid);
                continue;
            }

            try {
                SchedulerUtil.runForEntity(player, () -> clearFallbackEffect(player),
                        () -> fallbackEffects.remove(uuid));
            } catch (final RuntimeException ignored) {
                // Scheduling is unavailable while the plugin is disabling, clear inline instead.
                clearFallbackEffect(player);
            }
        }
    }

    @Nullable
    private EfficiencyMechanic getMechanic(final ItemStack item) {
        if (factory.isNotImplementedIn(item)) return null;
        return (EfficiencyMechanic) factory.getMechanic(item);
    }

    private void clear(final Player player) {
        breakSpeedModifier.clear(player);
        clearFallbackEffect(player);
    }

    private void applyFallbackEffect(final Player player, final EfficiencyMechanic mechanic) {
        final PotionEffectType type = mechanic.getType();
        if (type == null) return;

        clearFallbackEffect(player);
        final PotionEffect applied = new PotionEffect(type, FALLBACK_EFFECT_DURATION,
                mechanic.getAmount() - 1, false, false, false);
        // Snapshot any same-type effect from another source (e.g. beacon haste) so clearing
        // the fallback restores it instead of stripping the effect type entirely.
        fallbackEffects.put(player.getUniqueId(), new FallbackEffect(applied, player.getPotionEffect(type)));
        player.addPotionEffect(applied);
    }

    private void clearFallbackEffect(final Player player) {
        final FallbackEffect effect = fallbackEffects.remove(player.getUniqueId());
        if (effect == null) return;

        player.removePotionEffect(effect.applied().getType());
        if (effect.previous() != null) player.addPotionEffect(effect.previous());
    }

    private record FallbackEffect(PotionEffect applied, @Nullable PotionEffect previous) {}
}
