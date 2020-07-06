package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Drop {

    private final List<Loot> loots;
    final boolean silktouch;
    final boolean fortune;
    private final boolean hasMinimalTool;
    String minimalTool;
    final String sourceID;

    public Drop(List<Loot> loots, boolean silktouch, boolean fortune, String sourceID, Material minimalTool) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        hasMinimalTool = true;
        this.minimalTool = minimalTool.toString();
    }

    public Drop(List<Loot> loots, boolean silktouch, boolean fortune, String sourceID) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        hasMinimalTool = false;
    }

    final List<String> types = Arrays.asList("WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND", "NETHERITE");

    public boolean isToolEnough(ItemStack itemInHand) {
        if (!hasMinimalTool)
            return true;

        String type = itemInHand.getType().toString();
        if (type.contains("_")) {
            String[] typeArray = type.split("_");
            String[] minimalToolArray = minimalTool.split("_");
            if (typeArray[1].equals(minimalToolArray[1]))
                return (types.indexOf(typeArray[0]) >= types.indexOf(minimalToolArray[0]));
        }
        return false;

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
            fortuneMultiplier += new Random().nextInt(itemInHand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS));

        for (Loot loot : loots) {
            loot.dropNaturally(location, fortuneMultiplier);
        }
    }
}
