package io.th0rgal.oraxen.mechanics.provided.gameplay.durability;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

@Deprecated(forRemoval = true, since = "1.20.6")
public class DurabilityMechanicManager implements Listener {

    private final DurabilityMechanicFactory factory;

    public DurabilityMechanicManager(DurabilityMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamaged(PlayerItemDamageEvent event) {
        DurabilityMechanic mechanic = factory.getMechanic(event.getItem());
        if (mechanic == null || !mechanic.changeDurability(event.getPlayer(), event.getItem(), -event.getDamage())) return;
        event.setDamage(0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        DurabilityMechanic mechanic = factory.getMechanic(OraxenItems.getIdByItem(event.getItem()));
        if (mechanic == null || !mechanic.changeDurability(event.getPlayer(), event.getItem(), event.getRepairAmount())) return;
        event.setRepairAmount(0);
    }

    //TODO Support for getting custom durability from second and first if available instead of ItemMeta
    @EventHandler
    public void onItemRepair(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getFirstItem();
        ItemStack secondItem = event.getInventory().getSecondItem();
        ItemStack resultItem = event.getResult();
        if (firstItem == null || secondItem == null) return;
        if (!(firstItem.getItemMeta() instanceof Damageable)) return;
        if (!(secondItem.getItemMeta() instanceof Damageable)) return;

        DurabilityMechanic firstMechanic = factory.getMechanic(OraxenItems.getIdByItem(firstItem));
        DurabilityMechanic secondMechanic = factory.getMechanic(OraxenItems.getIdByItem(secondItem));
        // If not same mechanic or first has no mechanic, we return
        // We do not want to allow custom durab swords with base DIAMOND to repair DIAMOND
        if (firstMechanic != secondMechanic || firstMechanic == null) return;
        int combinedDurability = firstMechanic.getItemMaxDurability() - firstMechanic.getItemDurability(firstItem);
        combinedDurability += secondMechanic.getItemDurability(secondItem);
        double repairPercentAmount = firstMechanic.getItemMaxDurability() * 0.12;
        int repairAmount = (int) (repairPercentAmount + combinedDurability);

        DurabilityMechanic resultMechanic = factory.getMechanic(OraxenItems.getIdByItem(resultItem));
        if (resultMechanic == null) return;
        resultMechanic.changeDurability(InventoryUtils.playerFromView(event), resultItem, repairAmount);
        event.setResult(resultItem);
    }
}
