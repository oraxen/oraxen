package io.th0rgal.oraxen.compatibilities.provided.mcmmo;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    // Uses ConcurrentHashMap for Folia compatibility (concurrent region thread access)
    private final Map<String, BlockXPConfig> xpConfigCache = new ConcurrentHashMap<>();

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
     * Clears the XP config cache when Oraxen items are reloaded.
     * This ensures that changes to mcmmo XP settings take effect
     * immediately after /oraxen reload without requiring a server restart.
     */
    @EventHandler
    public void onItemsReloaded(OraxenItemsLoadedEvent event) {
        clearCache();
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

        // Check for sentinel value (no config) or null skill
        if (config == null || config == NO_CONFIG_SENTINEL || config.skill() == null) return;

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
     * Uses computeIfAbsent for thread-safe atomic access (Folia compatibility).
     *
     * @param itemId the Oraxen item ID
     * @return the XP config, or null if none configured
     */
    private BlockXPConfig getXPConfig(String itemId) {
        // Use computeIfAbsent for atomic thread-safe access
        // Note: ConcurrentHashMap does not allow null values, so we use a sentinel
        return xpConfigCache.computeIfAbsent(itemId, this::loadXPConfigOrSentinel);
    }

    // Sentinel value to represent "no config" since ConcurrentHashMap doesn't allow null values
    private static final BlockXPConfig NO_CONFIG_SENTINEL = new BlockXPConfig(null, -1);

    private BlockXPConfig loadXPConfigOrSentinel(String itemId) {
        BlockXPConfig config = loadXPConfig(itemId);
        return config != null ? config : NO_CONFIG_SENTINEL;
    }

    /**
     * Loads XP configuration from an Oraxen item's mechanic section.
     * <p>
     * The mcmmo section is a sibling of the block mechanic (noteblock/stringblock)
     * under the Mechanics parent, not a child of the block mechanic itself.
     * <pre>
     * mythril_ore:
     *   Mechanics:
     *     noteblock:    # mechanic.getSection() returns this
     *       ...
     *     mcmmo:        # we need getParent().getConfigurationSection("mcmmo")
     *       skill: MINING
     *       xp: 150
     * </pre>
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

        // The mechanic's section is e.g. "noteblock", we need to go up to "Mechanics"
        // and then get the sibling "mcmmo" section
        ConfigurationSection mechanicSection = mechanic.getSection();
        if (mechanicSection == null) return null;

        ConfigurationSection parentSection = mechanicSection.getParent();
        if (parentSection == null) return null;

        ConfigurationSection mcmmoSection = parentSection.getConfigurationSection("mcmmo");
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
     * Skill can be null for the NO_CONFIG_SENTINEL value.
     */
    private record BlockXPConfig(PrimarySkillType skill, int xp) {}
}
