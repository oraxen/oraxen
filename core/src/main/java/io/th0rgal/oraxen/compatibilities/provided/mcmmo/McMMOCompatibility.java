package io.th0rgal.oraxen.compatibilities.provided.mcmmo;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

/**
 * Compatibility provider for mcMMO integration.
 * <p>
 * This integration allows Oraxen custom blocks and items to work seamlessly
 * with mcMMO's skill system, including:
 * <ul>
 *   <li>XP rewards for breaking Oraxen custom blocks (Mining, Woodcutting, Herbalism, Excavation)</li>
 *   <li>Enhanced tool effects when mcMMO super abilities are active</li>
 *   <li>XP multiplier support for special Oraxen items</li>
 * </ul>
 * <p>
 * Configuration is done through the mechanics system. Each Oraxen item can specify
 * mcMMO-related settings using the {@code mcmmo} mechanic section.
 */
public class McMMOCompatibility extends CompatibilityProvider<Plugin> {

    private static McMMOCompatibility instance;
    private McMMOBlockXPHandler blockXPHandler;
    private McMMOAbilityListener abilityListener;

    public McMMOCompatibility() {
        instance = this;
    }

    @Override
    public void enable(String pluginName) {
        super.enable(pluginName);

        // Initialize handlers
        blockXPHandler = new McMMOBlockXPHandler();
        abilityListener = new McMMOAbilityListener();

        // Register the block XP handler
        Bukkit.getPluginManager().registerEvents(blockXPHandler, OraxenPlugin.get());
        Bukkit.getPluginManager().registerEvents(abilityListener, OraxenPlugin.get());

        Logs.logSuccess("mcMMO integration enabled! Oraxen blocks and items will now interact with mcMMO skills.");
    }

    @Override
    public void disable() {
        // Unregister the listeners before nulling them
        if (blockXPHandler != null) {
            HandlerList.unregisterAll(blockXPHandler);
            blockXPHandler.clearCache();
        }
        if (abilityListener != null) {
            HandlerList.unregisterAll(abilityListener);
            abilityListener.clear();
        }

        super.disable();
        instance = null;
        blockXPHandler = null;
        abilityListener = null;
    }

    /**
     * Gets the singleton instance of this compatibility provider.
     * 
     * @return the instance, or null if mcMMO integration is not active
     */
    public static McMMOCompatibility getInstance() {
        return instance;
    }

    /**
     * Checks if mcMMO integration is currently active.
     * 
     * @return true if mcMMO is loaded and integration is enabled
     */
    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    /**
     * Gets the block XP handler for programmatic access.
     * 
     * @return the block XP handler, or null if not active
     */
    public McMMOBlockXPHandler getBlockXPHandler() {
        return blockXPHandler;
    }

    /**
     * Gets the ability listener for programmatic access.
     * 
     * @return the ability listener, or null if not active
     */
    public McMMOAbilityListener getAbilityListener() {
        return abilityListener;
    }
}
