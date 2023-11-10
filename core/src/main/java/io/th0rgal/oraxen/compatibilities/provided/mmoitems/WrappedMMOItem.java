package io.th0rgal.oraxen.compatibilities.provided.mmoitems;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ItemTier;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WrappedMMOItem {
    private final Type type;
    private final String id;

    private final int level;
    private final ItemTier tier;

    public WrappedMMOItem(ConfigurationSection section) {
        if (!Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            Logs.logError("MMOItems is not installed");
            type = null;
            id = null;
            level = 0;
            tier = null;
        } else {
            type = MMOItems.plugin.getTypes().getOrThrow(section.getString("type"));
            id = section.getString("id");

            // Check if template exists
            MMOItems.plugin.getTemplates().getTemplateOrThrow(type, id);

            // Optional stuff
            level = section.getInt("level", 1);
            tier = section.isString("tier") ? MMOItems.plugin.getTiers().getOrThrow(section.getString("tier")) : null;
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
        try {
            return MMOItems.plugin.getTemplates().getTemplateOrThrow(type, id);
        } catch (Exception e) {
            Logs.logError("Failed to load MMOItem " + id);
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
            return null;
        }
    }

    public ItemStack build() {
        try {
            return new MMOItemBuilder(getTemplate(), level, tier).build().newBuilder().build();
        } catch (Exception e) {
            Logs.logError("Failed to load MMOItem " + id);
            if (!Bukkit.getPluginManager().isPluginEnabled("MMOItems"))
                Logs.logWarning("MMOItems is not installed");
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
            return null;
        }
    }
}
