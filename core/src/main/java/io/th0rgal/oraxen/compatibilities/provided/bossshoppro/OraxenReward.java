package io.th0rgal.oraxen.compatibilities.provided.bossshoppro;

import io.th0rgal.oraxen.api.OraxenItems;
import org.black_ixx.bossshop.core.BSBuy;
import org.black_ixx.bossshop.core.rewards.BSRewardType;
import org.black_ixx.bossshop.managers.ClassManager;
import org.black_ixx.bossshop.managers.misc.InputReader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class OraxenReward extends BSRewardType {

    @Override
    public Object createObject(Object o, boolean forceFinalState) {
        return OraxenItems.getItemStacksByName(InputReader.readStringListList(o));
    }

    @Override
    public boolean validityCheck(String itemName, Object reward) {
        return true;
    }

    @Override
    public void enableType() {

    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean canBuy(Player player, BSBuy bsBuy, boolean messageIfNoSuccess, Object reward,
        ClickType clickType) {
        if (!ClassManager.manager.getSettings().getInventoryFullDropItems()) {
            List<ItemStack> items = (List<ItemStack>) reward;
            if (!ClassManager.manager.getItemStackChecker().hasFreeSpace(player, items)) {
                if (messageIfNoSuccess) {
                    ClassManager.manager
                        .getMessageHandler()
                        .sendMessage("Main.InventoryFull", player, null, player, bsBuy.getShop(), null, bsBuy);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void giveReward(Player player, BSBuy bsBuy, Object reward, ClickType clickType) {
        List<ItemStack> itemStacks = (List<ItemStack>) reward;

        if (!(itemStacks.isEmpty())) {
            for (ItemStack itemStack : itemStacks)
                if (itemStack.getType() != Material.AIR)
                    ClassManager.manager
                        .getItemStackCreator()
                        .giveItem(player, bsBuy, itemStack, itemStack.getAmount(), true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getDisplayReward(Player player, BSBuy bsBuy, Object reward, ClickType clickType) {
        String itemsFormatted = ClassManager.manager
            .getItemStackTranslator()
            .getFriendlyText((List<ItemStack>) reward);
        return ClassManager.manager.getMessageHandler().get("Display.Item").replace("%items%", itemsFormatted);
    }

    @Override
    public String[] createNames() {
        return new String[] { "oraxen", "oraxen-item", "item-oraxen" };
    }

    @Override
    public boolean mightNeedShopUpdate() {
        return true;
    }

}
