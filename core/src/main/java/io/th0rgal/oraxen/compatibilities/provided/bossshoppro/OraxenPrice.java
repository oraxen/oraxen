package io.th0rgal.oraxen.compatibilities.provided.bossshoppro;

import io.th0rgal.oraxen.api.OraxenItems;
import org.black_ixx.bossshop.core.BSBuy;
import org.black_ixx.bossshop.core.prices.BSPriceType;
import org.black_ixx.bossshop.managers.ClassManager;
import org.black_ixx.bossshop.managers.misc.InputReader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class OraxenPrice extends BSPriceType {

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
    public boolean hasPrice(Player player, BSBuy bsBuy, Object reward, ClickType clickType, boolean messageOnFailure) {
        List<ItemStack> items = (List<ItemStack>) reward;
        for (ItemStack i : items) {
            if (!ClassManager.manager.getItemStackChecker().inventoryContainsItem(player, i, bsBuy)) {
                if (messageOnFailure)
                    ClassManager.manager.getMessageHandler().sendMessage("NotEnough.Item", player);
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String takePrice(Player player, BSBuy bsBuy, Object reward, ClickType clickType) {
        List<ItemStack> itemStacks = (List<ItemStack>) reward;
        if (!(itemStacks.isEmpty()))
            for (ItemStack itemStack : itemStacks)
                if (itemStack.getType() != Material.AIR)
                    ClassManager.manager.getItemStackChecker().takeItem(itemStack, player, bsBuy);

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getDisplayPrice(Player player, BSBuy bsBuy, Object reward, ClickType clickType) {
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
