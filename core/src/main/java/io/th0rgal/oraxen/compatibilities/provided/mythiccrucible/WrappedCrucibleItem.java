package io.th0rgal.oraxen.compatibilities.provided.mythiccrucible;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.items.MythicItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WrappedCrucibleItem {
    private final String id;

    public WrappedCrucibleItem(ConfigurationSection section) {
        id = section.getString("id");
    }

    public WrappedCrucibleItem(String id) {
        this.id = id;
    }

    @Nullable
    public ItemStack build() {
        try {
            Optional<MythicItem> maybeItem = MythicBukkit.inst().getItemManager().getItem(id);
            return BukkitAdapter.adapt(maybeItem.orElseThrow().generateItemStack(1));
        } catch (Exception e) {
            Logs.logError("Failed to load MythicCrucible item " + id);
            if (!PluginUtils.isEnabled("MythicCrucible"))
                Logs.logWarning("MythicCrucible is not installed");
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
            return null;
        }
    }
}
