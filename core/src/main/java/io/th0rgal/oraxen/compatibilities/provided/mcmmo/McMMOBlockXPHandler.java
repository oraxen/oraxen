package io.th0rgal.oraxen.compatibilities.provided.mcmmo;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles mcMMO XP rewards for breaking Oraxen custom blocks.
 * <p>
 * This handler listens to Oraxen block break events and awards appropriate
 * mcMMO skill XP based on the block's configuration. Server owners can
 * configure XP values per block in their Oraxen item configs.
 * <p>
 * Example configuration in an Oraxen item file:
 * <pre>
 * mythril_ore:
 *   Mechanics:
 *     noteblock:
 *       # ... other noteblock settings
 *     mcmmo:
 *       skill: MINING
 *       xp: 150
 * </pre>
 */
public class McMMOBlockXPHandler implements Listener {

    // Cache for block XP configurations to avoid repeated config lookups
    private final Map<String, BlockXPConfig> xpConfigCache = new HashMap<>();

    /**
     * Handles XP rewards when an Oraxen noteblock is broken.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNoteBlockBreak(OraxenNoteBlockBreakEvent event) {
        Player player = event.getPlayer();
        NoteBlockMechanic mechanic = event.getMechanic();

        if (player == null || mechanic == null) return;

        awardXPForBlock(player, mechanic);
    }

    /**
     * Handles XP rewards when an Oraxen stringblock is broken.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStringBlockBreak(OraxenStringBlockBreakEvent event) {
        Player player = event.getPlayer();
        StringBlockMechanic mechanic = event.getMechanic();

        if (player == null || mechanic == null) return;

        awardXPForBlock(player, mechanic);
    }

    /**
     * Awards mcMMO XP to a player for breaking an Oraxen block.
     *
     * @param player   the player who broke the block
     * @param mechanic the mechanic of the broken block
     */
    private void awardXPForBlock(Player player, Mechanic mechanic) {
        String itemId = mechanic.getItemID();
        BlockXPConfig config = getXPConfig(itemId);

        if (config == null) return;

        try {
            // Use mcMMO's API to add XP with proper handling
            ExperienceAPI.addXP(player, config.skill().name(), config.xp(), "PVE");
        } catch (Exception e) {
            // Log but don't spam - this could happen if mcMMO is in a weird state
            Logs.logWarning("Failed to award mcMMO XP for block " + itemId + ": " + e.getMessage());
        }
    }

    /**
     * Gets the XP configuration for a block, using cache when possible.
     *
     * @param itemId the Oraxen item ID
     * @return the XP config, or null if none configured
     */
    private BlockXPConfig getXPConfig(String itemId) {
        // Check cache first
        if (xpConfigCache.containsKey(itemId)) {
            return xpConfigCache.get(itemId);
        }

        // Try to load from config
        BlockXPConfig config = loadXPConfig(itemId);
        xpConfigCache.put(itemId, config);
        return config;
    }

    /**
     * Loads XP configuration from an Oraxen item's mechanic section.
     *
     * @param itemId the Oraxen item ID
     * @return the loaded config, or null if not configured
     */
    private BlockXPConfig loadXPConfig(String itemId) {
        // Get the mechanic section for this item
        Mechanic mechanic = OraxenBlocks.getNoteBlockMechanic(itemId);
        if (mechanic == null) {
            mechanic = OraxenBlocks.getStringBlockMechanic(itemId);
        }
        if (mechanic == null) return null;

        // Look for mcmmo configuration in the mechanic's section
        ConfigurationSection section = mechanic.getSection();
        if (section == null) return null;

        ConfigurationSection mcmmoSection = section.getConfigurationSection("mcmmo");
        if (mcmmoSection == null) return null;

        String skillName = mcmmoSection.getString("skill");
        int xp = mcmmoSection.getInt("xp", 0);

        if (skillName == null || xp <= 0) return null;

        try {
            PrimarySkillType skill = PrimarySkillType.valueOf(skillName.toUpperCase());
            return new BlockXPConfig(skill, xp);
        } catch (IllegalArgumentException e) {
            Logs.logWarning("Invalid mcMMO skill '" + skillName + "' configured for block " + itemId);
            return null;
        }
    }

    /**
     * Clears the XP configuration cache. Call this when configs are reloaded.
     */
    public void clearCache() {
        xpConfigCache.clear();
    }

    /**
     * Manually registers XP for a custom block. Useful for programmatic configuration.
     *
     * @param itemId the Oraxen item ID
     * @param skill  the mcMMO skill to award XP for
     * @param xp     the amount of XP to award
     */
    public void registerBlockXP(String itemId, PrimarySkillType skill, int xp) {
        xpConfigCache.put(itemId, new BlockXPConfig(skill, xp));
    }

    /**
     * Simple record to hold block XP configuration.
     */
    private record BlockXPConfig(PrimarySkillType skill, int xp) {}
}
