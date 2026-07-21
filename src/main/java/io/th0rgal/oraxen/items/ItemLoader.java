package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class ItemLoader {

    public static final Map<String, ModelData> MODEL_DATAS_BY_ID = new HashMap<>();

    private final OraxenMeta oraxenMeta;
    private final ConfigurationSection section;
    private final Material type;
    private WrappedMMOItem mmoItem;
    private WrappedCrucibleItem crucibleItem;
    private WrappedEcoItem ecoItem;
    private ItemLoader templateItem;

    public ItemLoader(final ConfigurationSection section) {
        this.section = section;

        if (section.isString("template"))
            templateItem = ItemTemplate.getLoaderTemplate(section.getString("template"));

        final ConfigurationSection crucibleSection = OraxenYaml.getConfigurationSection(section, "crucible");
        final ConfigurationSection mmoSection = OraxenYaml.getConfigurationSection(section, "mmoitem");
        final ConfigurationSection ecoItemSection = OraxenYaml.getConfigurationSection(section, "ecoitem");
        if (crucibleSection != null)
            crucibleItem = new WrappedCrucibleItem(crucibleSection);
        else if (section.isString("crucible_id"))
            crucibleItem = new WrappedCrucibleItem(section.getString("crucible_id"));
        else if (ecoItemSection != null)
            ecoItem = new WrappedEcoItem(ecoItemSection);
        else if (section.isString("ecoitem_id"))
            ecoItem = new WrappedEcoItem(section.getString("ecoitem_id"));
        else if (mmoSection != null)
            mmoItem = new WrappedMMOItem(mmoSection);

        Material material = OraxenYaml.getMaterial(section.getString("material", ""));
        if (material == null)
            material = usesTemplate() ? templateItem.type : Material.PAPER;
        type = material;

        oraxenMeta = templateItem != null ? templateItem.oraxenMeta : new OraxenMeta();
        if (OraxenYaml.isConfigurationSection(section, "Pack")) {
            final ConfigurationSection packSection = OraxenYaml.getConfigurationSection(section, "Pack");
            oraxenMeta.setPackInfos(packSection);
            assert packSection != null;
            if (packSection.isInt("custom_model_data"))
                MODEL_DATAS_BY_ID.put(section.getName(),
                        new ModelData(type, oraxenMeta.getModelName(), packSection.getInt("custom_model_data")));
        }
    }

    public boolean usesMMOItems() {
        return crucibleItem == null && ecoItem == null && mmoItem != null && mmoItem.build() != null;
    }

    public boolean usesCrucibleItems() {
        return mmoItem == null && ecoItem == null && crucibleItem != null && crucibleItem.build() != null;
    }

    public boolean usesEcoItems() {
        return mmoItem == null && crucibleItem == null && ecoItem != null && ecoItem.build() != null;
    }

    public boolean usesTemplate() {
        return templateItem != null;
    }

    public ItemValidator.Result load() {
        final ItemBuilder item;

        if (usesCrucibleItems())
            item = new ItemBuilder(crucibleItem);
        else if (usesMMOItems())
            item = new ItemBuilder(mmoItem);
        else if (usesEcoItems())
            item = new ItemBuilder(ecoItem);
        else
            item = new ItemBuilder(type);

        if (usesTemplate())
            templateItem.validator().validate(item);
        return validator().validate(item);
    }

    private ItemValidator validator() {
        return new ItemValidator(section, mergeWithTemplateSection(), type, oraxenMeta, MODEL_DATAS_BY_ID);
    }

    private ConfigurationSection mergeWithTemplateSection() {
        if (section == null || templateItem == null || templateItem.section == null)
            return section;

        final ConfigurationSection merged = new YamlConfiguration().createSection(section.getName());
        OraxenYaml.copyConfigurationSection(templateItem.section, merged);
        OraxenYaml.copyConfigurationSection(section, merged);
        merged.set("injectId", true);
        OraxenYaml.invalidateKeyCache(merged);
        return merged;
    }

}
