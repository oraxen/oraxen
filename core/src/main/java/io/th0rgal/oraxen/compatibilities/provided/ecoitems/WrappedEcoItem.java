package io.th0rgal.oraxen.compatibilities.provided.ecoitems;

import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.EcoItems;
import io.th0rgal.oraxen.utils.PluginUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WrappedEcoItem {
    private final String id;

    public WrappedEcoItem(ConfigurationSection section) {
        id = section.getString("id");
    }

    public WrappedEcoItem(String id) {
        this.id = id;
    }

    public ItemStack build() {
        if (id == null || !PluginUtils.isEnabled("EcoItems")) return null;
        EcoItem ecoItem = EcoItems.INSTANCE.getByID(id);
        return ecoItem != null ? ecoItem.getItemStack() : null;
    }
}
