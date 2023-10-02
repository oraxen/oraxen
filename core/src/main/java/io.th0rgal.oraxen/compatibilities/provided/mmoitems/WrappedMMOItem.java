package io.th0rgal.oraxen.compatibilities.provided.mmoitems;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ItemTier;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WrappedMMOItem {
    private final Type type;
    private final String id;

    private final int level;
    private final ItemTier tier;

    public WrappedMMOItem(ConfigurationSection section) {
        type = MMOItems.plugin.getTypes().getOrThrow(section.getString("type"));
        id = section.getString("id");

        // Check if template exists
        MMOItems.plugin.getTemplates().getTemplateOrThrow(type, id);

        // Optional stuff
        level = section.getInt("level", 1);
        tier = section.isString("tier") ? MMOItems.plugin.getTiers().getOrThrow(section.getString("tier")) : null;
    }

    private MMOItemTemplate getTemplate() {
        return MMOItems.plugin.getTemplates().getTemplateOrThrow(type, id);
    }

    public ItemStack build() {
        return new MMOItemBuilder(getTemplate(), level, tier).build().newBuilder().build();
    }
}
