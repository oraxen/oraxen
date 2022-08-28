package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanicFactory;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Drop {

    private List<String> hierarchy;
    private final List<Loot> loots;
    final boolean silktouch;
    final boolean fortune;
    private final boolean hasMinimalType;
    String minimalType;
    private final List<String> bestTools;
    final String sourceID;

    public Drop(List<String> hierarchy, List<Loot> loots, boolean silktouch, boolean fortune, String sourceID,
                String minimalType, List<String> bestTools) {
        this.hierarchy = hierarchy;
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        hasMinimalType = true;
        this.minimalType = minimalType;
        this.bestTools = bestTools;
    }

    public Drop(List<Loot> loots, boolean silktouch, boolean fortune, String sourceID) {
        this.loots = loots;
        this.silktouch = silktouch;
        this.fortune = fortune;
        this.sourceID = sourceID;
        hasMinimalType = false;
        this.bestTools = new ArrayList<>();
    }

    public String getItemType(ItemStack itemInHand) {
        String itemID = OraxenItems.getIdByItem(itemInHand);
        ItemTypeMechanicFactory factory = ItemTypeMechanicFactory.get();
        if (factory.isNotImplementedIn(itemID)) {
            String[] content = itemInHand.getType().toString().split("_");
            return content.length >= 2 ? content[0] : "";
        } else {
            ItemTypeMechanic mechanic = (ItemTypeMechanic) factory.getMechanic(itemID);
            return mechanic.itemType;
        }
    }

    public boolean canDrop(ItemStack itemInHand) {
        return isToolEnough(itemInHand) && isTypeEnough(itemInHand);
    }

    public boolean isTypeEnough(ItemStack itemInHand) {
        if (hasMinimalType) {
            String itemType = getItemType(itemInHand);
            return !itemType.isEmpty() && hierarchy.contains(itemType)
                    && (hierarchy.indexOf(itemType) >= hierarchy.indexOf(minimalType));
        }
        return true;
    }

    public boolean isToolEnough(ItemStack itemInHand) {
        if (!bestTools.isEmpty()) {
            String itemID = OraxenItems.getIdByItem(itemInHand);
            String type = itemInHand == null ? "AIR" : itemInHand.getType().toString().toUpperCase();
            if ((itemID != null && bestTools.contains(itemID.toUpperCase())
                    || bestTools.contains(type)))
                return true;
            else
                for (String toolName : bestTools)
                    if (type.endsWith(toolName.toUpperCase()))
                        return true;
            return false;
        }
        return true;
    }

    public int getDiff(ItemStack item) {
        return (minimalType == null) ? 0 : hierarchy.indexOf(getItemType(item)) - hierarchy.indexOf(minimalType);
    }

    public void spawns(Location location, ItemStack itemInHand) {
        if (!canDrop(itemInHand)) return;
        if (!location.isWorldLoaded()) return;

        int fortuneMultiplier = 1;
        if (itemInHand != null && itemInHand.hasItemMeta()) {
            if (silktouch && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
                location.getWorld().dropItemNaturally(location, OraxenItems.getItemById(sourceID).build());
                return;
            }

            if (fortune && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
                fortuneMultiplier += ThreadLocalRandom.current()
                        .nextInt(itemInHand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS));
        }

        for (Loot loot : loots) {
            loot.dropNaturally(location, fortuneMultiplier);
        }
    }

    public void furnitureSpawns(ItemFrame frame, ItemStack itemInHand) {
        ItemStack drop = OraxenItems.getItemById(sourceID).build();
        if (!canDrop(itemInHand)) return;
        if (!drop.hasItemMeta()) return;
        if (!frame.getLocation().isWorldLoaded()) return;

        if (frame.getItem().getItemMeta() instanceof LeatherArmorMeta leatherArmorMeta) {
            LeatherArmorMeta clone = (LeatherArmorMeta) drop.getItemMeta().clone();
            clone.setColor(leatherArmorMeta.getColor());
            drop.setItemMeta(clone);
        }

        if (frame.getItem().getItemMeta() instanceof PotionMeta potionMeta) {
            PotionMeta clone = (PotionMeta) drop.getItemMeta().clone();
            clone.setColor(potionMeta.getColor());
            drop.setItemMeta(clone);
        }

        frame.getLocation().getWorld().dropItemNaturally(frame.getLocation(), drop);
    }
}
