package io.th0rgal.oraxen.utils.drops;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
            String itemType = itemInHand == null ? "" : getItemType(itemInHand);
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

    public List<Loot> getLoots() {
        return loots;
    }

    public void spawns(Location location, ItemStack itemInHand) {
        if (!canDrop(itemInHand)) return;
        if (!location.isWorldLoaded()) return;

        int fortuneMultiplier = 1;
        if (itemInHand != null) {
            ItemMeta itemMeta = itemInHand.getItemMeta();
            if (itemMeta != null) {
                if (silktouch && itemMeta.hasEnchant(Enchantment.SILK_TOUCH)) {
                    if (location.getWorld() != null)
                        location.getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(location), OraxenItems.getItemById(sourceID).build());
                    return;
                }

                if (fortune && itemMeta.hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
                    fortuneMultiplier += ThreadLocalRandom.current()
                            .nextInt(itemMeta.getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS));
            }

        }

        for (Loot loot : loots) {
            loot.dropNaturally(location, fortuneMultiplier);
        }
    }

    public void furnitureSpawns(Entity baseEntity, ItemStack itemInHand) {
        ItemStack drop = OraxenItems.getItemById(sourceID).build();
        ItemMeta dropMeta = drop.getItemMeta();
        if (!canDrop(itemInHand)) return;
        if (dropMeta == null) return;
        if (!baseEntity.getLocation().isWorldLoaded()) return;

        if (FurnitureMechanic.getFurnitureItem(baseEntity).getItemMeta() instanceof LeatherArmorMeta leatherArmorMeta) {
            LeatherArmorMeta clone = (LeatherArmorMeta) dropMeta.clone();
            clone.setColor(leatherArmorMeta.getColor());
            drop.setItemMeta(clone);
        }

        if (FurnitureMechanic.getFurnitureItem(baseEntity).getItemMeta() instanceof PotionMeta potionMeta) {
            PotionMeta clone = (PotionMeta) dropMeta.clone();
            clone.setColor(potionMeta.getColor());
            drop.setItemMeta(clone);
        }

        baseEntity.getLocation().getWorld().dropItemNaturally(BlockHelpers.toCenterBlockLocation(baseEntity.getLocation()), drop);
    }
}
