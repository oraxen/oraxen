package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomBlockMiningListener implements Listener {

    private static final NamespacedKey BREAK_SPEED_KEY = NamespacedKey.fromString("oraxen:custom_breaking_speed");
    // Vanilla baseline: a hardness-1 block under a bare hand takes ~4.17 ticks per hit
    // (1 / 0.24 ≈ 4.17). Dividing 0.24 by the block hardness gives the per-tick fraction we
    // need to add to the player's BLOCK_BREAK_SPEED attribute to match the custom hardness.
    private static final double VANILLA_BREAK_SPEED_BASE = 0.24D;
    private static final double HARVESTABLE_BLOCK_DIVISOR = 30.0D;
    private static final double UNHARVESTABLE_BLOCK_DIVISOR = 100.0D;
    private static final double EXPECTED_NOTE_BLOCK_HARDNESS = 0.8D;
    private static final double FULL_BLOCK_MINING_COST = computeFullBlockMiningCost();
    private final Map<UUID, AttributeModifier> modifierMap = new ConcurrentHashMap<>();
    // Cache of the resolved AttributeModifier constructor (varies by server version).
    private static volatile ModifierFactory cachedModifierFactory;

    private static double computeFullBlockMiningCost() {
        double noteBlockHardness = Material.NOTE_BLOCK.getHardness();
        if (Math.abs(noteBlockHardness - EXPECTED_NOTE_BLOCK_HARDNESS) > 0.0001D) {
            Logs.logWarning("NOTE_BLOCK hardness is " + noteBlockHardness + " instead of " + EXPECTED_NOTE_BLOCK_HARDNESS + "; shaped-block mining speed compensation may need adjustment.");
        }
        return noteBlockHardness * HARVESTABLE_BLOCK_DIVISOR;
    }

    /**
     * Returns true if the BLOCK_BREAK_SPEED attribute is available on this server version.
     * The attribute was added in MC 1.20.5 - on older versions, the BreakerSystem
     * timer-based approach is used instead.
     */
    public static boolean isSupported() {
        return AttributeWrapper.BLOCK_BREAK_SPEED != null;
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(final BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final Block block = event.getBlock();
        final ItemStack tool = player.getInventory().getItemInMainHand();
        final MiningProfile miningProfile = getMiningProfile(block, tool);
        if (miningProfile == null) {
            removeTransientModifier(player);
            return;
        }

        final double hardness = miningProfile.hardness();
        if (hardness <= 0.0D) {
            removeTransientModifier(player);
            event.setInstaBreak(true);
            return;
        }

        final AttributeModifier modifier = createBreakingModifier(player, miningProfile);
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
    private MiningProfile getMiningProfile(final Block block, final ItemStack tool) {
        if (block.getType() == Material.NOTE_BLOCK) {
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
            if (mechanic == null) return null;
            if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock()) {
                mechanic = mechanic.getDirectional().getParentMechanic();
                if (mechanic == null) return null;
            }
            return mechanic.hasHardness(tool) ? new MiningProfile(block, mechanic.getHardness(tool),
                    mechanic.getAttributeSpeedMultiplier(tool, block.getType()), false) : null;
        }

        if (block.getType() == Material.TRIPWIRE) {
            final StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            return mechanic != null && mechanic.hasHardness(tool) ? new MiningProfile(block, mechanic.getHardness(tool),
                    mechanic.getAttributeSpeedMultiplier(tool, block.getType()), false) : null;
        }

        if (block.getType() == Material.CHORUS_PLANT) {
            final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
            return mechanic != null && mechanic.hasHardness(tool) ? new MiningProfile(block, mechanic.getHardness(tool),
                    mechanic.getAttributeSpeedMultiplier(tool, block.getType()), false) : null;
        }

        final ShapedBlockMechanic shapedMechanic = OraxenBlocks.getShapedMechanic(block);
        if (shapedMechanic != null) {
            if (shapedMechanic.hasHardness(tool)) return new MiningProfile(block, shapedMechanic.getHardness(tool),
                    shapedMechanic.getAttributeSpeedMultiplier(tool, block.getType()), true);
        }

        return null;
    }

    @Nullable
    private AttributeModifier createBreakingModifier(final Player player, final MiningProfile miningProfile) {
        final Attribute blockBreakSpeed = AttributeWrapper.BLOCK_BREAK_SPEED;
        if (BREAK_SPEED_KEY == null || blockBreakSpeed == null) return null;

        double speedFactor = VANILLA_BREAK_SPEED_BASE / miningProfile.hardness() * miningProfile.speedMultiplier();
        if (miningProfile.normalizeNativeMiningCost()) {
            speedFactor *= nativeMiningCostMultiplier(player.getInventory().getItemInMainHand(), miningProfile.block());
        }

        speedFactor = Math.max(0.01D, speedFactor);
        return instantiateModifier(speedFactor - 1.0D);
    }

    private double nativeMiningCostMultiplier(final ItemStack tool, final Block block) {
        final double fullBlockMiningCost = FULL_BLOCK_MINING_COST > 0.0D
                ? FULL_BLOCK_MINING_COST
                : HARVESTABLE_BLOCK_DIVISOR;
        final double nativeHardness = Math.max(0.0D, block.getType().getHardness());
        if (nativeHardness <= 0.0D) return 1.0D;

        return nativeHardness * nativeMiningDivisor(tool, block) / fullBlockMiningCost;
    }

    private double nativeMiningDivisor(final ItemStack tool, final Block block) {
        final Material blockType = block.getType();
        return canHarvest(blockType, tool) ? HARVESTABLE_BLOCK_DIVISOR : UNHARVESTABLE_BLOCK_DIVISOR;
    }

    private boolean canHarvest(final Material blockType, final ItemStack tool) {
        if (!requiresCorrectTool(blockType)) return true;
        if (tool == null) return false;

        final Material toolType = tool.getType();
        final String toolName = toolType.name();
        final String mineableTag = mineableTagName(toolName);
        return mineableTag != null && isTagged(blockType, mineableTag) && hasRequiredTier(blockType, toolName);
    }

    private boolean requiresCorrectTool(final Material blockType) {
        return isTagged(blockType, "needs_stone_tool")
                || isTagged(blockType, "needs_iron_tool")
                || isTagged(blockType, "needs_diamond_tool");
    }

    private boolean hasRequiredTier(final Material blockType, final String toolName) {
        if (isTagged(blockType, "needs_diamond_tool")) {
            return toolName.startsWith("DIAMOND_") || toolName.startsWith("NETHERITE_");
        }
        if (isTagged(blockType, "needs_iron_tool")) {
            return toolName.startsWith("IRON_") || toolName.startsWith("DIAMOND_") || toolName.startsWith("NETHERITE_");
        }
        if (isTagged(blockType, "needs_stone_tool")) {
            return toolName.startsWith("STONE_") || toolName.startsWith("IRON_")
                    || toolName.startsWith("DIAMOND_") || toolName.startsWith("NETHERITE_");
        }
        return true;
    }

    @Nullable
    private String mineableTagName(final String toolName) {
        if (toolName.endsWith("_PICKAXE")) return "mineable/pickaxe";
        if (toolName.endsWith("_AXE")) return "mineable/axe";
        if (toolName.endsWith("_SHOVEL")) return "mineable/shovel";
        if (toolName.endsWith("_HOE")) return "mineable/hoe";
        return null;
    }

    private boolean isTagged(final Material blockType, final String tagName) {
        final Tag<Material> tag = org.bukkit.Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
        return tag != null && tag.isTagged(blockType);
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
        ModifierFactory factory = cachedModifierFactory;
        if (factory == null) {
            factory = resolveModifierFactory();
            cachedModifierFactory = factory;
        }
        return factory == null ? null : factory.create(amount);
    }

    @Nullable
    private ModifierFactory resolveModifierFactory() {
        try {
            final Class<?> slotGroupClass = Class.forName("org.bukkit.inventory.EquipmentSlotGroup");
            final Object handGroup = slotGroupClass.getField("HAND").get(null);
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    NamespacedKey.class, double.class, AttributeModifier.Operation.class, slotGroupClass);
            return amount -> {
                try {
                    return constructor.newInstance(BREAK_SPEED_KEY, amount,
                            AttributeModifier.Operation.MULTIPLY_SCALAR_1, handGroup);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            };
        } catch (ReflectiveOperationException ignored) {
            // Fall through to older constructor variants.
        }

        try {
            final Constructor<AttributeModifier> constructor = AttributeModifier.class.getConstructor(
                    UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class);
            return amount -> {
                try {
                    return constructor.newInstance(
                            UUID.nameUUIDFromBytes(BREAK_SPEED_KEY.asString().getBytes()),
                            BREAK_SPEED_KEY.getKey().toLowerCase(Locale.ROOT),
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
            return amount -> {
                try {
                    return constructor.newInstance(
                            UUID.nameUUIDFromBytes(BREAK_SPEED_KEY.asString().getBytes()),
                            BREAK_SPEED_KEY.getKey().toLowerCase(Locale.ROOT),
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
        @Nullable AttributeModifier create(double amount);
    }

    private record MiningProfile(Block block, double hardness, double speedMultiplier, boolean normalizeNativeMiningCost) {}
}
