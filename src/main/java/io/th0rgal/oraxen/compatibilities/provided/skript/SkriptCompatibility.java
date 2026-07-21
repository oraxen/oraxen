package io.th0rgal.oraxen.compatibilities.provided.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.IOException;

/**
 * Skript compatibility provider for Oraxen.
 * Registers Oraxen expressions, events, conditions, and effects for use in Skript scripts.
 */
public class SkriptCompatibility extends CompatibilityProvider<Skript> {

    private static SkriptAddon addon;

    public SkriptCompatibility() {
        try {
            addon = Skript.registerAddon(OraxenPlugin.get());
            registerTypes();
            loadSyntaxClasses();
            Logs.logSuccess("Skript compatibility enabled - Oraxen syntax registered");
        } catch (Exception e) {
            Logs.logError("Failed to register Skript addon: " + e.getMessage());
            if (OraxenPlugin.get().getConfigsManager().getSettings().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Register custom types for Skript
     */
    private void registerTypes() {
        OraxenSkriptTypes.register();
    }

    /**
     * Load all Skript syntax classes (expressions, events, conditions, effects)
     * @throws IOException if loading fails
     */
    private void loadSyntaxClasses() throws IOException {
        addon.loadClasses("io.th0rgal.oraxen.compatibilities.provided.skript",
                "expressions", "events", "conditions", "effects");
    }

    @Override
    public void disable() {
        super.disable();
        addon = null;
    }

    /**
     * Get the Skript addon instance
     * @return The SkriptAddon for Oraxen, or null if not initialized
     */
    public static SkriptAddon getAddon() {
        return addon;
    }
}
