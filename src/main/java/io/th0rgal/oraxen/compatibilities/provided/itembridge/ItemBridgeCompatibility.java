package io.th0rgal.oraxen.compatibilities.provided.itembridge;

import com.jojodmo.itembridge.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ItemBridgeCompatibility extends CompatibilityProvider<Main> implements ItemBridgeListener {

    public ItemBridgeCompatibility() {
        this(true);
    }

    public ItemBridgeCompatibility(boolean setup) {
        if (setup)
            setup(OraxenPlugin.get());
    }

    public static void setup(Plugin plugin) {
        ItemBridge bridge = new ItemBridge(plugin, "oraxen", "oxn", "o");
        bridge.registerListener(new ItemBridgeCompatibility(false));
    }

    @Override
    public ItemBridgeListenerPriority getPriority() {
        return ItemBridgeListenerPriority.MEDIUM;
    }

    @Override
    public ItemStack fetchItemStack(@NotNull String item) {
        return OraxenItems.exists(item) ? OraxenItems.getItemById(item).build() : null;
    }

    @Override
    public String getItemName(@NotNull ItemStack stack) {
        return OraxenItems.getIdByItem(stack);
    }

    @Override
    public boolean isItem(@NotNull ItemStack stack, String name) {
        return name.equals(getItemName(stack));
    }
}
