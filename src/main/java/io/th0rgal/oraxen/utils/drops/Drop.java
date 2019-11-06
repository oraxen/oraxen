package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class Drop {

    private final List<Loot> loots;
    final boolean silktouch;
    private final boolean hasMinimalTool;
    String minimalTool;
    final String sourceID;

    public Drop(List<Loot> loots, boolean silktouch, String sourceID, Material minimalTool) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.sourceID = sourceID;
        hasMinimalTool = true;
        this.minimalTool = minimalTool.toString();
    }

    public Drop(List<Loot> loots, boolean silktouch, String sourceID) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.sourceID = sourceID;
        hasMinimalTool = false;
    }

    final List<String> types = Arrays.asList("WOODEN", "STONE", "GOLDEN", "DIAMOND");

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

        if (silktouch) {
            location.getWorld().dropItemNaturally(location, OraxenItems.getItemById(sourceID).build());
            return;
        }
        for (Loot loot : loots)
            loot.dropNaturally(location);
    }
}
