package io.th0rgal.oraxen.compatibilities.provided.itembridge;

import com.jojodmo.itembridge.ItemBridge;
import com.jojodmo.itembridge.ItemBridgeListener;
import com.jojodmo.itembridge.ItemBridgeListenerPriority;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class OraxenItemBridge implements ItemBridgeListener {

    public static void setup(Plugin plugin) {
        ItemBridge bridge = new ItemBridge(plugin, "oraxen", "oxn", "o");
        bridge.registerListener(new OraxenItemBridge());
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
