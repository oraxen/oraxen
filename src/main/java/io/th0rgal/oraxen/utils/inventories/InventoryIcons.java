package io.th0rgal.oraxen.utils.inventories;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.Material;

final class InventoryIcons {

    private InventoryIcons() {
    }

    static ItemBuilder resolve(Settings setting, String bundledId, Material fallback) {
        ItemBuilder configured = itemFromSetting(setting);
        if (configured != null) return configured;

        ItemBuilder bundled = OraxenItems.getItemById(bundledId);
        if (bundled != null) return bundled.clone();

        return new ItemBuilder(fallback);
    }

    private static ItemBuilder itemFromSetting(Settings setting) {
        Object value = setting.getValue();
        if (value == null) return null;

        String id = value.toString().trim();
        if (id.isEmpty()) return null;

        ItemBuilder oraxenItem = OraxenItems.getItemById(id);
        if (oraxenItem != null) return oraxenItem.clone();

        Material material = Material.matchMaterial(id);
        return material != null && material.isItem() ? new ItemBuilder(material) : null;
    }
}
