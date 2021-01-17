package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.itemtype.ItemTypeMechanic;
import io.th0rgal.oraxen.mechanics.provided.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class Drop {

    private List<String> hierarchy;
    private final List<Loot> loots;
    final boolean silktouch;
    final boolean fortune;
    private final boolean hasMinimalType;
    String minimalType;
    final String sourceID;

    public Drop(List<String> hierarchy, List<Loot> loots, boolean silktouch, boolean fortune, String sourceID, String minimalType) {
        this.hierarchy = hierarchy;
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        hasMinimalType = true;
        this.minimalType = minimalType;
    }

    public Drop(List<Loot> loots, boolean silktouch, boolean fortune, String sourceID) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        hasMinimalType = false;
    }

    public boolean isToolEnough(ItemStack itemInHand) {
        if (!hasMinimalType)
            return true;

        String itemID = OraxenItems.getIdByItem(itemInHand);
        ItemTypeMechanicFactory factory = ItemTypeMechanicFactory.get();
        String itemType;
        if (factory.isNotImplementedIn(itemID)) {
            itemType = itemInHand.getType().toString().split("_")[0];
            if (!hierarchy.contains(itemType))
                return false;
        } else {
            ItemTypeMechanic mechanic = (ItemTypeMechanic) factory.getMechanic(itemID);
            itemType = mechanic.itemType;
        }
        return (hierarchy.indexOf(itemType) >= hierarchy.indexOf(minimalType));
    }

    public void spawns(Location location, ItemStack itemInHand) {
        if (!isToolEnough(itemInHand))
            return;

        if (silktouch && itemInHand.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            location.getWorld().dropItemNaturally(location, OraxenItems.getItemById(sourceID).build());
            return;
        }

        int fortuneMultiplier = 1;
        if (fortune && itemInHand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
            fortuneMultiplier += new Random()
                    .nextInt(itemInHand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS));

        for (Loot loot : loots) {
            loot.dropNaturally(location, fortuneMultiplier);
        }
    }
}
