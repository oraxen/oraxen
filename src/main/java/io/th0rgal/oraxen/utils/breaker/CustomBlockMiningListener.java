package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
    private final BreakSpeedModifier breakSpeedModifier = new BreakSpeedModifier(BREAK_SPEED_KEY);

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
        return BreakSpeedModifier.isSupported();
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCustomBlock(final BlockDamageEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final Block block = event.getBlock();
        final ItemStack tool = player.getInventory().getItemInMainHand();
        final MiningProfile miningProfile = getMiningProfile(block, tool);
        if (miningProfile == null) {
            breakSpeedModifier.clear(player);
            return;
        }

        final double hardness = miningProfile.hardness();
        if (hardness <= 0.0D) {
            breakSpeedModifier.clear(player);
            event.setInstaBreak(true);
            return;
        }

        breakSpeedModifier.apply(player, breakSpeedMultiplier(player, miningProfile));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageAbort(final BlockDamageAbortEvent event) {
        breakSpeedModifier.clear(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        breakSpeedModifier.clear(event.getPlayer());
    }

    @EventHandler
    public void onDisconnect(final PlayerQuitEvent event) {
        breakSpeedModifier.clear(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(final PlayerSwapHandItemsEvent event) {
        breakSpeedModifier.clear(event.getPlayer());
    }

    @EventHandler
    public void onDropHand(final PlayerDropItemEvent event) {
        breakSpeedModifier.clear(event.getPlayer());
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

    private double breakSpeedMultiplier(final Player player, final MiningProfile miningProfile) {
        double speedFactor = VANILLA_BREAK_SPEED_BASE / miningProfile.hardness() * miningProfile.speedMultiplier();
        if (miningProfile.normalizeNativeMiningCost()) {
            speedFactor *= nativeMiningCostMultiplier(player.getInventory().getItemInMainHand(), miningProfile.block());
        }

        return Math.max(0.01D, speedFactor);
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

    private record MiningProfile(Block block, double hardness, double speedMultiplier, boolean normalizeNativeMiningCost) {}
}
