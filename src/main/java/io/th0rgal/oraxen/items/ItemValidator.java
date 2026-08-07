package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

public final class ItemValidator {

    private final ConfigurationSection section;
    private final ConfigurationSection mergedSection;
    private final Material type;
    private final OraxenMeta oraxenMeta;
    private final Map<String, ModelData> modelDatasById;
    private final ItemMigrator migrator;

    public ItemValidator(final ConfigurationSection section, final ConfigurationSection mergedSection,
            final Material type, final OraxenMeta oraxenMeta, final Map<String, ModelData> modelDatasById) {
        this.section = section;
        this.mergedSection = mergedSection;
        this.type = type;
        this.oraxenMeta = oraxenMeta;
        this.modelDatasById = modelDatasById;
        migrator = new ItemMigrator(section);
    }

    public Result validate(final ItemBuilder item) {
        try {
            final ItemProperties properties = new ItemProperties(section, type, oraxenMeta, modelDatasById);
            migrator.recordLegacyNameMigration(properties.applyBasic(item));
            new ItemComponents(section, type).apply(item, mergedSection);
            properties.applyMiscAndVanilla(item, mergedSection);
            new ItemMechanics(section, migrator).apply(item, mergedSection);
            properties.applyAppearance(item);
            item.setOraxenMeta(oraxenMeta);
        } catch (final Exception e) {
            final String itemId = section != null ? section.getName() : "unknown";
            Logs.logError("Error building item \"" + itemId + "\"");
            Logs.logError(e.getMessage());
            Logs.debug(e);
        }
        return new Result(item, migrator.configUpdated(), migrator.blockConfigMigrated());
    }

    public record Result(ItemBuilder item, boolean configUpdated, boolean blockConfigMigrated) {}
}
