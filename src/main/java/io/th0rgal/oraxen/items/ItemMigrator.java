package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.MiningConfigMigration;
import io.th0rgal.oraxen.configs.ClickActionsMigration;
import io.th0rgal.oraxen.configs.FurnitureConfigMigration;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
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
        migrateUppercaseSections();
        migrateEnchantable();
        if (FurnitureConfigMigration.migrate(section)) {
            configUpdated = true;
            blockConfigMigrated = true;
        }
        if (section != null && ClickActionsMigration.migrate(OraxenYaml.getConfigurationSection(section, "mechanics"))) {
            configUpdated = true;
            blockConfigMigrated = true;
        }
        if (section != null && MiningConfigMigration.migrateItem(section)) {
            configUpdated = true;
            blockConfigMigrated = true; // Reuse the migration backup path before rewriting the item file.
            if (OraxenPlugin.get() != null)
                Logs.logWarning("Item " + section.getName()
                        + " uses deprecated mechanics.bigmining; migrated to mechanics.mining with world-relative offsets.");
        }
    }

    /**
     * Migrates the item-level sections that historically used capitalized names
     * to their lowercase canonical names.
     */
    public void migrateUppercaseSections() {
        if (section == null)
            return;

        for (final String key : section.getKeys(false).toArray(String[]::new)) {
            final String lowercaseKey = key.toLowerCase(Locale.ROOT);
            if (key.equals(lowercaseKey))
                continue;
            if (!switch (lowercaseKey) {
                case "mechanics", "pack", "components" -> true;
                default -> false;
            })
                continue;

            final Object value = section.get(key);
            final Object existingValue = section.get(lowercaseKey);
            if (value instanceof ConfigurationSection sourceSection) {
                final ConfigurationSection targetSection;
                if (existingValue instanceof ConfigurationSection existingSection) {
                    targetSection = existingSection;
                } else {
                    if (existingValue != null)
                        section.set(lowercaseKey, null);
                    targetSection = section.createSection(lowercaseKey);
                }
                OraxenYaml.copyConfigurationSection(sourceSection, targetSection);
                OraxenYaml.invalidateKeyCache(targetSection);
            } else if (existingValue == null) {
                section.set(lowercaseKey, value);
            }

            section.set(key, null);
            OraxenYaml.invalidateKeyCache(section);
            configUpdated = true;
        }
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

    private void migrateEnchantable() {
        if (section == null || !section.contains("disable_enchanting"))
            return;

        if (!section.contains("enchantable"))
            section.set("enchantable", !section.getBoolean("disable_enchanting"));
        section.set("disable_enchanting", null);
        OraxenYaml.invalidateKeyCache(section);
        configUpdated = true;
        blockConfigMigrated = true;
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

            mechanicsSection.set(legacySection.getName(), null);
            OraxenYaml.invalidateKeyCache(mechanicsSection);
            OraxenYaml.invalidateKeyCache(blockSection);
            configUpdated = true;
            blockConfigMigrated = true;
            if (OraxenPlugin.get() != null)
                Logs.logWarning("Item " + section.getName() + " uses legacy mechanics." + legacyMechanicID
                        + "; it has been migrated to mechanics.block.");
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
