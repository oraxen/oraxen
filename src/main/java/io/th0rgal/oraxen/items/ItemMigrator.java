package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

public final class ItemMigrator {

    private static final Map<String, String> LEGACY_BLOCK_MECHANIC_TYPES = Map.of(
            "noteblock", "FULL",
            "stringblock", "STRING",
            "chorusblock", "CHORUS",
            "shaped_block", "STAIR"
    );

    private final ConfigurationSection section;
    private boolean configUpdated;
    private boolean blockConfigMigrated;

    public ItemMigrator(final ConfigurationSection section) {
        this.section = section;
    }

    public void recordLegacyNameMigration(final boolean migrated) {
        configUpdated |= migrated;
    }

    /**
     * Marks the backing item config as changed so the caller persists it, used when
     * an automatically assigned custom model data is written back into the item file.
     */
    public void markConfigUpdated() {
        configUpdated = true;
    }

    public void migrateLegacyBlockMechanics(final ConfigurationSection mechanicsSection) {
        if (OraxenYaml.getConfigurationSection(mechanicsSection, "block") != null)
            return;

        for (final Map.Entry<String, String> legacyMechanic : LEGACY_BLOCK_MECHANIC_TYPES.entrySet()) {
            final String legacyMechanicID = legacyMechanic.getKey();
            final ConfigurationSection legacySection = OraxenYaml.getConfigurationSection(mechanicsSection, legacyMechanicID);
            if (legacySection == null)
                continue;

            final ConfigurationSection blockSection = mechanicsSection.createSection("block");
            OraxenYaml.copyConfigurationSection(legacySection, blockSection);
            if (!blockSection.contains("type"))
                blockSection.set("type", legacyMechanic.getValue());

            mechanicsSection.set(legacyMechanicID, null);
            OraxenYaml.invalidateKeyCache(mechanicsSection);
            OraxenYaml.invalidateKeyCache(blockSection);
            configUpdated = true;
            blockConfigMigrated = true;
            if (OraxenPlugin.get() != null)
                Logs.logWarning("Item " + section.getName() + " uses legacy Mechanics." + legacyMechanicID
                        + "; it has been migrated to Mechanics.block.");
            return;
        }
    }

    public boolean configUpdated() {
        return configUpdated;
    }

    public boolean blockConfigMigrated() {
        return blockConfigMigrated;
    }
}
