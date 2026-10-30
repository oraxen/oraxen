package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemProperties;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.CustomModelDataHelper;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Internal render variants for placed furniture, separate from its inventory item. */
public final class FurnitureAppearance {
    private final String id;
    private final ConfigurationSection appearance;
    private final @Nullable Vector armorStandScale;
    private final Map<Material, Map<String, ItemBuilder>> renderItems = new EnumMap<>(Material.class);

    public FurnitureAppearance(String itemId, ConfigurationSection appearance, @Nullable Vector armorStandScale) {
        this.id = "furniture/" + itemId;
        this.appearance = new MemoryConfiguration();
        OraxenYaml.copyConfigurationSection(appearance, this.appearance);
        this.armorStandScale = armorStandScale != null ? armorStandScale.clone() : null;
    }

    public boolean hasStage(String stage) {
        ConfigurationSection models = appearance.getConfigurationSection("models");
        return models != null && models.isString(stage);
    }

    public synchronized Map<String, ItemBuilder> getRenderItems(Material material) {
        return renderItems.computeIfAbsent(material, this::buildRenderItems);
    }

    private Map<String, ItemBuilder> buildRenderItems(Material material) {
        Map<String, ItemBuilder> variants = new LinkedHashMap<>();
        variants.put(id, buildRenderItem(material, id, null));
        ConfigurationSection models = appearance.getConfigurationSection("models");
        if (models != null) {
            for (String stage : models.getKeys(false)) {
                String model = models.getString(stage);
                if (model != null)
                    variants.put(id + "/" + stage, buildRenderItem(material, id + "/" + stage, model));
            }
        }
        return Collections.unmodifiableMap(variants);
    }

    private ItemBuilder buildRenderItem(Material material, String variantId, @Nullable String stageModel) {
        ConfigurationSection item = new MemoryConfiguration().createSection(variantId);
        ConfigurationSection modelItem = new MemoryConfiguration().createSection(id.substring("furniture/".length()));
        ConfigurationSection pack = modelItem.createSection("pack");
        OraxenYaml.copyConfigurationSection(appearance, pack);
        pack.set("models", null);
        pack.set("custom_model_data", null);
        if (stageModel != null) {
            pack.set("model", stageModel);
            pack.set("generate_model", false);
        }
        OraxenMeta meta = new OraxenMeta();
        meta.setPackInfos(pack);
        if (armorStandScale != null && !meta.shouldGenerateModel()) {
            // A wrapper inherits the external model without changing its inventory head transform.
            pack.set("parent_model", meta.getModelName());
            pack.set("model", variantId);
            pack.set("generate_model", true);
            pack.set("textures", null);
            meta = new OraxenMeta();
            meta.setPackInfos(pack);
        }
        if (meta.shouldGenerateModel()) meta.setModelName(variantId);
        if (armorStandScale != null) meta.setArmorStandHeadScale(armorStandScale.clone());

        ModelData modelData;
        synchronized (ModelData.DATAS) {
            int customModelData = ModelData.generateId(variantId, material);
            // Keep these internal variants separate from inventory model identifiers.
            modelData = new ModelData(material, variantId, customModelData);
        }
        ItemBuilder builder = new ItemBuilder(material);
        builder.setOraxenMeta(meta);
        new ItemProperties(item, material, meta, Map.of(variantId, modelData)).applyAppearance(builder);
        return builder;
    }

    public ItemStack apply(ItemStack original, @Nullable String stage) {
        ItemStack result = original.clone();
        ItemBuilder variant = getRenderItems(original.getType()).get(stage != null ? id + "/" + stage : id);
        if (variant == null) return result;
        ItemMeta source = variant.build().getItemMeta();
        ItemMeta target = result.getItemMeta();
        if (source == null || target == null) return result;
        if (VersionUtil.atOrAbove("1.21.2"))
            target.setItemModel(source.hasItemModel() ? source.getItemModel() : null);
        CustomModelDataHelper.setCustomModelData(target, CustomModelDataHelper.getCustomModelData(source));
        CustomModelDataHelper.ComponentData component = CustomModelDataHelper.getComponentData(source);
        CustomModelDataHelper.setComponentData(target,
                component != null ? component.strings() : List.of(),
                component != null ? component.floats() : List.of());
        result.setItemMeta(target);
        return result;
    }
}
