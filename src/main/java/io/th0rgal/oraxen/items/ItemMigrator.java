package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.MiningConfigMigration;
import io.th0rgal.oraxen.configs.ClickActionsMigration;
import io.th0rgal.oraxen.configs.FurnitureConfigMigration;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemMigrator {

    private static final Map<String, String> LEGACY_BLOCK_MECHANIC_TYPES = Map.of(
            "noteblock", "FULL",
            "stringblock", "STRING",
            "chorusblock", "CHORUS",
            "shaped_block", "STAIR"
    );
    private static final Map<String, List<String>> LEGACY_INVULNERABLE_CAUSES = Map.of(
            "burns_in_fire", List.of("fire", "fire_tick"),
            "burns_in_lava", List.of("lava"),
            "breaks_from_cactus", List.of("contact")
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
        if (section != null)
            migrateLegacyInvulnerability(OraxenYaml.getConfigurationSection(section, "mechanics"));
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
                } else if (existingValue != null) {
                    if (OraxenPlugin.get() != null)
                        Logs.logWarning("Item " + section.getName() + " keeps " + key
                                + " because " + lowercaseKey + " is not a configuration section.");
                    continue;
                } else {
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

    private void migrateLegacyInvulnerability(final ConfigurationSection mechanicsSection) {
        if (mechanicsSection == null)
            return;

        final Set<String> causes = new LinkedHashSet<>();
        addCauses(causes, section.get("invulnerable"));

        boolean migrated = false;
        final Object mechanicValue = OraxenYaml.getIgnoreCase(mechanicsSection, "invulnerable");
        if (mechanicValue instanceof List<?>) {
            addCauses(causes, mechanicValue);
            for (final String key : mechanicsSection.getKeys(false).toArray(String[]::new)) {
                if (key.equalsIgnoreCase("invulnerable"))
                    mechanicsSection.set(key, null);
            }
            migrated = true;
        }

        final ConfigurationSection miscSection = OraxenYaml.getConfigurationSection(mechanicsSection, "misc");
        if (miscSection != null) {
            for (final String key : miscSection.getKeys(false).toArray(String[]::new)) {
                final List<String> legacyCauses = LEGACY_INVULNERABLE_CAUSES.get(key.toLowerCase(Locale.ROOT));
                if (legacyCauses == null)
                    continue;
                if (!OraxenYaml.getBoolean(miscSection, key, true))
                    causes.addAll(legacyCauses);
                miscSection.set(key, null);
                migrated = true;
            }
            if (miscSection.getKeys(false).isEmpty())
                mechanicsSection.set(miscSection.getName(), null);
            OraxenYaml.invalidateKeyCache(miscSection);
        }
        if (!migrated)
            return;

        if (!causes.isEmpty())
            section.set("invulnerable", new ArrayList<>(causes));
        OraxenYaml.invalidateKeyCache(mechanicsSection);
        OraxenYaml.invalidateKeyCache(section);
        configUpdated = true;
        blockConfigMigrated = true;
    }

    private static void addCauses(final Set<String> causes, final Object raw) {
        if (!(raw instanceof List<?> entries))
            return;
        for (final Object entry : entries) {
            if (entry != null)
                causes.add(String.valueOf(entry).toLowerCase(Locale.ROOT));
        }
    }

    public boolean configUpdated() {
        return configUpdated;
    }

    public boolean blockConfigMigrated() {
        return blockConfigMigrated;
    }
}
