package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;

public final class MiningConfigMigration {

    private MiningConfigMigration() {
    }

    public static boolean migrateFactory(ConfigurationSection mechanicsConfig) {
        ConfigurationSection legacy = OraxenYaml.getConfigurationSection(mechanicsConfig, "bigmining");
        if (legacy == null) return false;

        ConfigurationSection mining = OraxenYaml.getConfigurationSection(mechanicsConfig, "mining");
        if (mining == null) mining = mechanicsConfig.createSection("mining");
        for (String key : legacy.getKeys(false)) {
            if (!mining.contains(key)) mining.set(key, legacy.get(key));
        }
        mechanicsConfig.set(legacy.getName(), null);
        OraxenYaml.invalidateKeyCache(mechanicsConfig);
        return true;
    }

    public static boolean migrateItem(ConfigurationSection item) {
        ConfigurationSection mechanics = OraxenYaml.getConfigurationSection(item, "mechanics");
        if (mechanics == null) return false;
        ConfigurationSection legacy = OraxenYaml.getConfigurationSection(mechanics, "bigmining");
        if (legacy == null) return false;

        if (!OraxenYaml.contains(mechanics, "mining")) {
            ConfigurationSection mining = mechanics.createSection("mining");
            // Legacy depth followed the targeted face, so keep radius and depth instead of
            // baking them into world-axis offsets.
            mining.set("radius", Math.max(0, legacy.getInt("radius")));
            mining.set("depth", Math.max(0, legacy.getInt("depth")));
        }

        mechanics.set(legacy.getName(), null);
        OraxenYaml.invalidateKeyCache(mechanics);
        return true;
    }
}
