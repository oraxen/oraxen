package io.th0rgal.oraxen.compatibility.cratereloaded;

import com.google.common.collect.Lists;
import com.hazebyte.crate.api.CrateAPI;
import com.hazebyte.crate.api.CratePlugin;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.api.event.PluginReadyEvent;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class CrateReloadedListener implements Listener {

    public void registerEvents(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerItems();
    }

    @EventHandler
    public void onPluginReady(PluginReadyEvent event) {
        registerItems();
    }

    private void registerItems() {
        CratePlugin cratePlugin = CrateAPI.getInstance();
        for (Crate crate : cratePlugin.getCrateRegistrar().getCrates()) {
            for (Reward reward : crate.getRewards()) {
                if (reward.getLine().getRewardString().contains("oraxen-item")) {
                    String rs = reward.getLine().getRewardString();
                    String rf = rs.substring(rs.indexOf("oraxen-item:(") + "oraxen-item:(".length());
                    String value = rf.substring(0, rf.indexOf(")"));
                    String itemID = value.split(" ")[0];
                    ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
                    if (itemBuilder == null) {
                        Message.ITEM_NOT_FOUND.logError(itemID);
                        continue;
                    }
                    ItemStack itemStack = itemBuilder.build();
                    if (value.split(" ").length > 1)
                        itemStack.setAmount(Integer.parseInt(value.split(" ")[1]));
                    reward.setItems(Lists.newArrayList(itemStack));
                }
            }
        }
    }


}
