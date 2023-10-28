package io.th0rgal.oraxen.compatibilities.provided.mythiccrucible;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythiccrucible.MythicCrucible;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WrappedCrucibleItem {
    private final String id;

    public WrappedCrucibleItem(ConfigurationSection section) {
        id = section.getString("id");
    }

    public WrappedCrucibleItem(String id) {
        this.id = id;
    }

    public ItemStack build() {
        try(MythicBukkit crucible = MythicCrucible.core()) {
            return crucible.getItemManager().getItemStack(id);
        } catch (Exception e) {
            Logs.logError("Failed to load MythicCrucible item " + id);
            if (Settings.DEBUG.toBool()) Logs.logWarning(e.getMessage());
            return null;
        }
    }
}
