package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomBlockMiningListener implements Listener {

    private static final NamespacedKey BREAK_SPEED_KEY = NamespacedKey.fromString("oraxen:custom_breaking_speed");
    private final Map<UUID, AttributeModifier> modifierMap = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(final BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final Block block = event.getBlock();
        final Double hardness = getHardness(block);
        if (hardness == null) {
            removeTransientModifier(player);
            return;
        }

        if (hardness <= 0.0D) {
            removeTransientModifier(player);
            event.setInstaBreak(true);
            return;
        }

        final AttributeModifier modifier = createBreakingModifier(hardness);
        if (modifier == null) {
            removeTransientModifier(player);
            return;
        }

        addTransientModifier(player, modifier);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageAbort(final BlockDamageAbortEvent event) {
        removeTransientModifier(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        removeTransientModifier(event.getPlayer());
    }

    @EventHandler
    public void onDisconnect(final PlayerQuitEvent event) {
        removeTransientModifier(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(final PlayerSwapHandItemsEvent event) {
        removeTransientModifier(event.getPlayer());
    }

    @EventHandler
    public void onDropHand(final PlayerDropItemEvent event) {
        removeTransientModifier(event.getPlayer());
    }

    @Nullable
    private Double getHardness(final Block block) {
        if (block.getType() == Material.NOTE_BLOCK) {
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
            if (mechanic == null) return null;
            if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock()) {
                mechanic = mechanic.getDirectional().getParentMechanic();
                if (mechanic == null) return null;
            }
            return mechanic.hasHardness() ? (double) mechanic.getHardness() : null;
        }

        if (block.getType() == Material.TRIPWIRE) {
            final StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            return mechanic != null && mechanic.hasHardness() ? (double) mechanic.getHardness() : null;
        }

        if (block.getType() == Material.CHORUS_PLANT) {
            final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
            return mechanic != null && mechanic.hasHardness() ? (double) mechanic.getHardness() : null;
        }

        return null;
    }

    @Nullable
    private AttributeModifier createBreakingModifier(final double hardness) {
        final Attribute blockBreakSpeed = AttributeWrapper.BLOCK_BREAK_SPEED;
        if (BREAK_SPEED_KEY == null || blockBreakSpeed == null) return null;

        // Rescale legacy Oraxen hardness values so 1 is close to normal mining speed and
        // larger values stay usable without becoming excessively slow.
        final double speedFactor = Math.max(0.05D, 1.2D / hardness);
        return instantiateModifier(speedFactor - 1.0D);
    }

    private void addTransientModifier(final Player player, final AttributeModifier modifier) {
        removeTransientModifier(player);
        modifierMap.put(player.getUniqueId(), modifier);

        final Attribute blockBreakSpeed = AttributeWrapper.BLOCK_BREAK_SPEED;
        if (blockBreakSpeed == null) return;

        final AttributeInstance attributeInstance = player.getAttribute(blockBreakSpeed);
        if (attributeInstance == null) return;

        try {
            final Method addTransientModifier = AttributeInstance.class
                    .getMethod("addTransientModifier", AttributeModifier.class);
            addTransientModifier.invoke(attributeInstance, modifier);
        } catch (ReflectiveOperationException ignored) {
            attributeInstance.addModifier(modifier);
        }
    }

    private void removeTransientModifier(final Player player) {
        final AttributeModifier modifier = modifierMap.remove(player.getUniqueId());
        if (modifier == null) return;

        final Attribute blockBreakSpeed = AttributeWrapper.BLOCK_BREAK_SPEED;
        if (blockBreakSpeed == null) return;

        final AttributeInstance attributeInstance = player.getAttribute(blockBreakSpeed);
        if (attributeInstance != null) attributeInstance.removeModifier(modifier);
    }

    @Nullable
    private AttributeModifier instantiateModifier(final double amount) {
        try {
            final Class<?> slotGroupClass = Class.forName("org.bukkit.inventory.EquipmentSlotGroup");
            final Object handGroup = slotGroupClass.getField("HAND").get(null);
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    NamespacedKey.class, double.class, AttributeModifier.Operation.class, slotGroupClass);
            return constructor.newInstance(BREAK_SPEED_KEY, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1, handGroup);
        } catch (ReflectiveOperationException ignored) {
            // Fall through to older constructor variants.
        }

        try {
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class);
            return constructor.newInstance(
                    UUID.nameUUIDFromBytes(BREAK_SPEED_KEY.asString().getBytes()),
                    BREAK_SPEED_KEY.getKey().toLowerCase(Locale.ROOT),
                    amount,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                    EquipmentSlot.HAND
            );
        } catch (ReflectiveOperationException ignored) {
            // Fall through to the oldest constructor variant.
        }

        try {
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class);
            return constructor.newInstance(
                    UUID.nameUUIDFromBytes(BREAK_SPEED_KEY.asString().getBytes()),
                    BREAK_SPEED_KEY.getKey().toLowerCase(Locale.ROOT),
                    amount,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
