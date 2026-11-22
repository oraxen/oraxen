package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;

/** Converts soulbound lose_chance values written as a fraction from 0 to 1 into a percentage. */
public final class SoulboundConfigMigration {

    private SoulboundConfigMigration() {
    }

    /**
     * Only values strictly between 0 and 1 are treated as fractions. 0 means the same in both formats,
     * and 1 or more is already a percentage. Converted values never drop below 1, so they are not
     * converted again on the next load.
     */
    public static boolean migrateItem(ConfigurationSection item) {
        ConfigurationSection mechanics = OraxenYaml.getConfigurationSection(item, "mechanics");
        ConfigurationSection soulbound = OraxenYaml.getConfigurationSection(mechanics, "soulbound");
        if (soulbound == null || !(soulbound.get("lose_chance") instanceof Number loseChance)) return false;
        double fraction = loseChance.doubleValue();
        if (!(fraction > 0 && fraction < 1)) return false;

        BigDecimal percent = BigDecimal.valueOf(fraction).movePointRight(2).max(BigDecimal.ONE).stripTrailingZeros();
        soulbound.set("lose_chance", percent.scale() <= 0 ? (Object) percent.intValue() : percent.doubleValue());
        return true;
    }
}
