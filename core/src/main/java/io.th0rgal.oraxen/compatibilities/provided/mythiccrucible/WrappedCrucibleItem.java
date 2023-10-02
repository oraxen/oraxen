package io.th0rgal.oraxen.compatibilities.provided.mythiccrucible;

import io.lumine.mythiccrucible.MythicCrucible;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class WrappedCrucibleItem {
    private final String id;

    public WrappedCrucibleItem(ConfigurationSection section) {
        id = section.getString("id");
    }

    public ItemStack build() {
        return MythicCrucible.core().getItemManager().getItemStack(id);
    }
}
