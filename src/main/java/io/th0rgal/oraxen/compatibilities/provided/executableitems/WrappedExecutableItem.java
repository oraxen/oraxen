package io.th0rgal.oraxen.compatibilities.provided.executableitems;

import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemInterface;
import io.th0rgal.oraxen.utils.PluginUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WrappedExecutableItem {
    private final String id;

    public WrappedExecutableItem(ConfigurationSection section) {
        this.id = section.getString("id");
    }

    public WrappedExecutableItem(String id) {
        this.id = id;
    }

    @Nullable
    public ItemStack build() {
        if (id == null || !PluginUtils.isEnabled("ExecutableItems")) return null;

        Optional<ExecutableItemInterface> eiOpt = ExecutableItemsAPI
                .getExecutableItemsManager()
                .getExecutableItem(id);

        if (eiOpt.isPresent()) {
            return eiOpt.get().buildItem(1, Optional.empty(), Optional.empty());
        }

        return null;
    }
}
