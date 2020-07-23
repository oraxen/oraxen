package io.th0rgal.oraxen.compatibility.bossshoppro;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Message;
import org.black_ixx.bossshop.core.BSBuy;
import org.black_ixx.bossshop.core.prices.BSPriceType;
import org.black_ixx.bossshop.managers.ClassManager;
import org.black_ixx.bossshop.managers.misc.InputReader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OraxenPrice extends BSPriceType {

    @Override
    public Object createObject(Object o, boolean force_final_state) {
        return getOraxenItems(InputReader.readStringListList(o));
    }

    @Override
    public boolean validityCheck(String item_name, Object reward) {
        return true;
    }

    @Override
    public void enableType() {
    }

    @Override
    public boolean hasPrice(Player player, BSBuy bsBuy, Object reward, ClickType clickType, boolean messageOnFailure) {
        List<ItemStack> items = (List<ItemStack>) reward;
        for (ItemStack i : items) {
            if (!ClassManager.manager.getItemStackChecker().inventoryContainsItem(player, i, bsBuy)) {
                if (messageOnFailure) {
                    ClassManager.manager.getMessageHandler().sendMessage("NotEnough.Item", player);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public String takePrice(Player player, BSBuy bsBuy, Object reward, ClickType clickType) {
        List<ItemStack> itemStacks = (List<ItemStack>) reward;
        if (!(itemStacks.isEmpty()))
            for (ItemStack itemStack : itemStacks)
                if (itemStack.getType() != Material.AIR)
                    ClassManager.manager.getItemStackChecker().takeItem(itemStack, player, bsBuy);

        return null;
    }

    @Override
    public String getDisplayPrice(Player player, BSBuy bsBuy, Object reward, ClickType clickType) {
        String items_formatted = ClassManager.manager.getItemStackTranslator().getFriendlyText((List<ItemStack>) reward);
        return ClassManager.manager.getMessageHandler().get("Display.Item").replace("%items%", items_formatted);
    }

    @Override
    public String[] createNames() {
        return new String[]{"oraxen", "oraxen-item", "item-oraxen"};
    }

    @Override
    public boolean mightNeedShopUpdate() {
        return true;
    }

    public List<ItemStack> getOraxenItems(List<List<String>> lists) {
        List<ItemStack> itemStacks = new ArrayList<>();
        for (List<String> itemsList : lists) {
            ItemStack itemStack = new ItemStack(Material.AIR);
            for (String line : itemsList) {
                String[] params = line.split(":");
                if (params[0].equalsIgnoreCase("type")) {
                    if (OraxenItems.isAnItem(params[1]))
                        itemStack = OraxenItems.getItemById(params[1]).build().clone();
                    else {
                        Message.ITEM_NOT_FOUND.logError(params[1]);
                        break;
                    }
                }
                if (params[0].equalsIgnoreCase("amount")) {
                    itemStack.setAmount(Integer.parseInt(params[1]));
                }
            }
            itemStacks.add(itemStack);
        }
        return itemStacks;
    }
}
