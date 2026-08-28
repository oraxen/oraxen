package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a transient {@code BLOCK_BREAK_SPEED} modifier to players, tracked per player so it can
 * be removed again. Each instance owns one {@link NamespacedKey}, so several independent modifiers
 * can stack on the same player without overwriting each other.
 *
 * <p>The attribute only exists on 1.20.5+, callers that need a fallback on older servers should
 * check {@link #isSupported()} first.</p>
 */
public final class BreakSpeedModifier {

    // The AttributeModifier(NamespacedKey, double, Operation, EquipmentSlotGroup) constructor was
    // added in 1.21; 1.20.5/1.20.6 only offer the UUID-based constructors (resolved reflectively
    // below), even though EquipmentSlotGroup itself already exists there.
    private static final boolean SUPPORTS_KEYED_MODIFIER = VersionUtil.atOrAbove("1.21");
    // Cache of the resolved AttributeModifier constructor (varies by server version).
    private static volatile ModifierFactory cachedModifierFactory;

    private final NamespacedKey key;
    private final Map<UUID, AttributeModifier> modifierMap = new ConcurrentHashMap<>();

    public BreakSpeedModifier(@Nullable final NamespacedKey key) {
        this.key = key;
    }

    /**
     * Returns true if the BLOCK_BREAK_SPEED attribute is available on this server version.
     * The attribute was added in MC 1.20.5.
     */
    public static boolean isSupported() {
        return AttributeWrapper.BLOCK_BREAK_SPEED != null;
    }

    /**
     * Replaces any modifier this instance previously applied to the player with one scaling their
     * mining speed by the given multiplier, where {@code 1.0} is vanilla speed.
     *
     * @return true if the modifier was applied
     */
    public boolean apply(final Player player, final double multiplier) {
        clear(player);
        if (key == null || !isSupported()) return false;

        final AttributeModifier modifier = instantiateModifier(key, multiplier - 1.0D);
        if (modifier == null) return false;

        modifierMap.put(player.getUniqueId(), modifier);

        final AttributeInstance attributeInstance = attributeInstance(player);
        if (attributeInstance == null) return false;

        try {
            final Method addTransientModifier = AttributeInstance.class
                    .getMethod("addTransientModifier", AttributeModifier.class);
            addTransientModifier.invoke(attributeInstance, modifier);
        } catch (ReflectiveOperationException ignored) {
            attributeInstance.addModifier(modifier);
        }
        return true;
    }

    /**
     * Removes this instance's modifier from every player it is still applied to, each on the thread
     * owning that player. Used when the owning listener is discarded (mechanic reload) so no stale
     * mining-speed modifier survives.
     */
    public void clearAll() {
        for (final UUID uuid : List.copyOf(modifierMap.keySet())) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                modifierMap.remove(uuid);
                continue;
            }

            try {
                SchedulerUtil.runForEntity(player, () -> clear(player), () -> modifierMap.remove(uuid));
            } catch (final RuntimeException ignored) {
                // Scheduling is unavailable while the plugin is disabling, clear inline instead.
                clear(player);
            }
        }
    }

    /**
     * Removes the modifier this instance applied to the player, if any.
     */
    public void clear(final Player player) {
        final AttributeModifier modifier = modifierMap.remove(player.getUniqueId());
        if (modifier == null) return;

        final AttributeInstance attributeInstance = attributeInstance(player);
        if (attributeInstance != null) attributeInstance.removeModifier(modifier);
    }

    @Nullable
    private AttributeInstance attributeInstance(final Player player) {
        final Attribute blockBreakSpeed = AttributeWrapper.BLOCK_BREAK_SPEED;
        return blockBreakSpeed == null ? null : player.getAttribute(blockBreakSpeed);
    }

    @Nullable
    private static AttributeModifier instantiateModifier(final NamespacedKey key, final double amount) {
        ModifierFactory factory = cachedModifierFactory;
        if (factory == null) {
            factory = resolveModifierFactory();
            cachedModifierFactory = factory;
        }
        return factory == null ? null : factory.create(key, amount);
    }

    @Nullable
    private static ModifierFactory resolveModifierFactory() {
        if (SUPPORTS_KEYED_MODIFIER)
            return (key, amount) -> new AttributeModifier(key, amount,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.HAND);

        try {
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class);
            return (key, amount) -> {
                try {
                    return constructor.newInstance(
                            UUID.nameUUIDFromBytes(key.asString().getBytes()),
                            key.getKey().toLowerCase(Locale.ROOT),
                            amount,
                            AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                            EquipmentSlot.HAND);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            };
        } catch (ReflectiveOperationException ignored) {
            // Fall through to the oldest constructor variant.
        }

        try {
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class);
            return (key, amount) -> {
                try {
                    return constructor.newInstance(
                            UUID.nameUUIDFromBytes(key.asString().getBytes()),
                            key.getKey().toLowerCase(Locale.ROOT),
                            amount,
                            AttributeModifier.Operation.MULTIPLY_SCALAR_1);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            };
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface ModifierFactory {
        @Nullable AttributeModifier create(NamespacedKey key, double amount);
    }
}
