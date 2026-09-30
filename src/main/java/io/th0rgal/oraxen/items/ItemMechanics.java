package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public final class ItemMechanics {

    private static final Set<String> LEGACY_BLOCK_MECHANIC_IDS = Set.of(
            "noteblock", "stringblock", "chorusblock", "shaped_block");

    private final ConfigurationSection section;
    private final ItemMigrator migrator;

    public ItemMechanics(final ConfigurationSection section, final ItemMigrator migrator) {
        this.section = section;
        this.migrator = migrator;
    }

    public void apply(final ItemBuilder item, final ConfigurationSection mergedSection) {
        final ConfigurationSection mechanicsSection = mergedSection.getConfigurationSection("mechanics");
        if (mechanicsSection == null)
            return;

        migrator.migrateLegacyBlockMechanics(mechanicsSection);

        ItemBuilder modifiedItem = item;
        for (final String mechanicID : mechanicsSection.getKeys(false)) {
            final MechanicFactory factory = MechanicsManager.getMechanicFactory(mechanicID);
            if (factory == null) {
                if (LEGACY_BLOCK_MECHANIC_IDS.contains(mechanicID.toLowerCase(Locale.ROOT)))
                    Logs.logWarning("Item " + section.getName() + " uses legacy mechanics." + mechanicID
                            + "; migrate it to mechanics.block or this mechanic will be ignored.");
                continue;
            }

            final Object mechanicConfig = mechanicsSection.get(mechanicID);
            final Mechanic mechanic;
            if (mechanicConfig instanceof List<?> entries)
                mechanic = factory.parse(section.getName(), entries);
            else {
                final ConfigurationSection mechanicSection = OraxenYaml.getConfigurationSection(mechanicsSection, mechanicID);
                if (mechanicSection == null)
                    continue;
                mechanic = factory.parse(mechanicSection);
            }
            if (mechanic == null)
                continue;

            for (final Function<ItemBuilder, ItemBuilder> itemModifier : mechanic.getItemModifiers())
                modifiedItem = itemModifier.apply(modifiedItem);
        }
    }
}
