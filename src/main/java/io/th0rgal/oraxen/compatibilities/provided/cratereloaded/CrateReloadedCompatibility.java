package io.th0rgal.oraxen.compatibilities.provided.cratereloaded;

import com.hazebyte.crate.api.CrateAPI;
import com.hazebyte.crate.api.CratePlugin;
import com.hazebyte.crate.api.crate.Crate;
import com.hazebyte.crate.api.crate.reward.Reward;
import com.hazebyte.crate.api.event.PluginReadyEvent;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CrateReloadedCompatibility extends CompatibilityProvider<CratePlugin> {

    public CrateReloadedCompatibility() {
        super();
        try {
            registerItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPluginReady(PluginReadyEvent event) {
        registerItems();
    }

    private void registerItems() {
        CratePlugin cratePlugin = CrateAPI.getInstance();
        for (Crate crate : cratePlugin.getCrateRegistrar().getCrates()) {
            solveRewards(crate.getRewards());
            solveRewards(crate.getConstantRewards());
        }
    }

    private void solveRewards(List<Reward> rewards) {
        for (Reward reward : rewards) {
            if (reward.getLine().getRewardString().contains("oraxen-item")) {
                List<ItemStack> items = reward.getItems();
                items.addAll(getOraxenItems(reward.getLine().getRewardString(), "oraxen-item"));
                reward.setItems(items);
            }
            if (reward.getLine().getRewardString().contains("oraxen-display")) {
                List<ItemStack> items = getOraxenItems(reward.getLine().getRewardString(), "oraxen-display");
                if (!items.isEmpty())
                    reward.setDisplayItem(items.get(0));
            }
        }
    }

    private List<ItemStack> getOraxenItems(String rs, String paramName) {
        String rewardString = rs + "";
        List<ItemStack> items = new ArrayList<>();
        while (rewardString.contains(paramName)) {
            rewardString = rewardString.substring(rewardString.indexOf(paramName + ":(") + (paramName + ":(").length());
            String value = rewardString.substring(0, rewardString.indexOf(")"));
            rewardString = rewardString.substring(rewardString.indexOf(")") + 1);
            String itemID = value.split(" ")[0];
            if (!OraxenItems.exists(itemID)) {
                Message.ITEM_NOT_FOUND.log(Utils.tagResolver("item", itemID));
                continue;
            }
            ItemBuilder itemBuilder = OraxenItems.getItemById(itemID);
            ItemStack itemStack = itemBuilder.build();
            if (value.split(" ").length > 1)
                itemStack.setAmount(Integer.parseInt(value.split(" ")[1]));
            items.add(itemStack);
        }
        return items;
    }

}
