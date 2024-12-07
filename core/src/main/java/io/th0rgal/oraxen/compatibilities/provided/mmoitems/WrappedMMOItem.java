package io.th0rgal.oraxen.compatibilities.provided.mmoitems;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.PluginUtils;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ItemTier;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WrappedMMOItem {
    private final Type type;
    private final String id;

    private final int level;
    private final ItemTier tier;

    public WrappedMMOItem(ConfigurationSection section) {
        if (!PluginUtils.isEnabled("MMOItems")) {
            Message.MMOITEMS_NOT_INSTALLED.log();
            type = null;
            id = null;
            level = 0;
            tier = null;
        } else {
            type = MMOItems.plugin.getTypes().get(section.getString("type"));
            id = section.getString("id");

            // Check if template exists
            if (!MMOItems.plugin.getTemplates().hasTemplate(type, id)) {
                Message.MMOITEMS_LOADING_ITEM_FAILED.log(AdventureUtils.tagResolver("id", id));
                Message.MMOITEMS_MISSING_TEMPLATE.log();
            }

            // Optional stuff
            level = section.getInt("level", 1);
            String tierId = section.getString("tier");
            tier = tierId != null && MMOItems.plugin.getTiers().has(tierId) ? MMOItems.plugin.getTiers().get(tierId)
                    : null;
        }
    }

    public WrappedMMOItem(Type type, String id, int level, ItemTier tier) {
        this.type = type;
        this.id = id;
        this.level = level;
        this.tier = tier;
    }

    public WrappedMMOItem(Type type, String id) {
        this.type = type;
        this.id = id;
        this.level = 1;
        this.tier = null;
    }

    private MMOItemTemplate getTemplate() {
        if (MMOItems.plugin.getTemplates().hasTemplate(type, id)) {
            return MMOItems.plugin.getTemplates().getTemplate(type, id);
        } else {
            Message.MMOITEMS_LOADING_ITEM_FAILED.log(AdventureUtils.tagResolver("id", id));
            Message.MMOITEMS_MISSING_TEMPLATE.log();
            return null;
        }
    }

    public ItemStack build() {
        if (PluginUtils.isEnabled("MMOItems")) {
            MMOItemTemplate template = getTemplate();
            if (template == null) {
                Message.MMOITEMS_LOADING_ITEM_FAILED.log(AdventureUtils.tagResolver("id", id));
                Message.MMOITEMS_MISSING_ITEM.log();
            } else
                return template.newBuilder().build().newBuilder().build();
        } else
            Message.MMOITEMS_MISSING_PLUGIN.log();
        return null;
    }
}
