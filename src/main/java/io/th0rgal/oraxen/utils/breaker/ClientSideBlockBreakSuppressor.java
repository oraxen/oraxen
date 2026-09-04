package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.utils.PotionUtils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Prevents a client from completing a vanilla block break while Oraxen owns the break progress.
 * The effects are sent only to the client; the server-side player and its mining calculations are
 * left unchanged.
 */
final class ClientSideBlockBreakSuppressor {

    // Resolve through the compatibility helper because older Bukkit versions exposed these under
    // the FAST_DIGGING/SLOW_DIGGING names.
    private static final PotionEffectType HASTE = PotionUtils.getEffectType("haste");
    private static final PotionEffectType MINING_FATIGUE = PotionUtils.getEffectType("mining_fatigue");
    private static final int CLIENT_ONLY_DURATION = Integer.MAX_VALUE;
    // These Paper methods were introduced after 1.20.1. Resolve them reflectively so Oraxen can
    // still load on its oldest supported server version, where the legacy breaker remains in use.
    private static final Method SEND_EFFECT_CHANGE = playerMethod("sendPotionEffectChange", org.bukkit.entity.LivingEntity.class, PotionEffect.class);
    private static final Method SEND_EFFECT_REMOVE = playerMethod("sendPotionEffectChangeRemove", org.bukkit.entity.LivingEntity.class, PotionEffectType.class);

    private ClientSideBlockBreakSuppressor() {
    }

    static boolean isSupported() {
        return SEND_EFFECT_CHANGE != null && SEND_EFFECT_REMOVE != null;
    }

    static void suppress(final Player player) {
        send(player, MINING_FATIGUE, 9);
        send(player, HASTE, 0);
    }

    static void restore(final Player player) {
        restore(player, MINING_FATIGUE);
        restore(player, HASTE);
    }

    private static void send(final Player player, @Nullable final PotionEffectType type, final int amplifier) {
        if (type == null || SEND_EFFECT_CHANGE == null) return;
        invoke(SEND_EFFECT_CHANGE, player, player,
                new PotionEffect(type, CLIENT_ONLY_DURATION, amplifier, false, false, false));
    }

    private static void restore(final Player player, @Nullable final PotionEffectType type) {
        if (type == null || SEND_EFFECT_CHANGE == null || SEND_EFFECT_REMOVE == null) return;

        invoke(SEND_EFFECT_REMOVE, player, player, type);
        final PotionEffect serverEffect = player.getPotionEffect(type);
        if (serverEffect != null) invoke(SEND_EFFECT_CHANGE, player, player, serverEffect);
    }

    @Nullable
    private static Method playerMethod(final String name, final Class<?>... parameterTypes) {
        try {
            return Player.class.getMethod(name, parameterTypes);
        } catch (final NoSuchMethodException ignored) {
            return null;
        }
    }

    private static void invoke(final Method method, final Player player, final Object... arguments) {
        try {
            method.invoke(player, arguments);
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not update the player's client-side mining effects", exception);
        }
    }
}
