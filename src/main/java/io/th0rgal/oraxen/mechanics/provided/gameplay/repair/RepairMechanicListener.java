package io.th0rgal.oraxen.mechanics.provided.gameplay.repair;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanicFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Deprecated(forRemoval = true, since = "1.21")
public class RepairMechanicListener implements Listener {
    private final RepairMechanicFactory factory;

    public RepairMechanicListener(MechanicFactory factory) {
        this.factory = (RepairMechanicFactory) factory;
    }

    @EventHandler
    public void onRepairItem(InventoryClickEvent event) {
        ItemStack item = event.getCursor();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID)) return;

        DurabilityMechanicFactory durabilityFactory = DurabilityMechanicFactory.get();
        RepairMechanic repairMechanic = (RepairMechanic) factory.getMechanic(itemID);

        ItemStack toRepair = event.getCurrentItem();
        if (toRepair == null) return;

        String toRepairId = OraxenItems.getIdByItem(toRepair);
        ItemMeta toRepairMeta = toRepair.getItemMeta();

        if (!(toRepairMeta instanceof Damageable damageable)) return;

        if (durabilityFactory.isNotImplementedIn(toRepairId)) {
            if (factory.isOraxenDurabilityOnly()) return;
            if (damageable.getDamage() == 0) return;

            damageable.setDamage(repairMechanic.getFinalDamage(toRepair.getType().getMaxDurability(), damageable.getDamage()));
        } else {
            DurabilityMechanic durabilityMechanic = (DurabilityMechanic) durabilityFactory.getMechanic(toRepairId);
            PersistentDataContainer pdc = toRepairMeta.getPersistentDataContainer();

            int realMaxDurability = durabilityMechanic.getItemMaxDurability();
            int damage = realMaxDurability - pdc.getOrDefault(DurabilityMechanic.DURABILITY_KEY, PersistentDataType.INTEGER, 0);
            if (damage == 0) return;
            int finalDamage = repairMechanic.getFinalDamage(realMaxDurability, damage);

            pdc.set(DurabilityMechanic.DURABILITY_KEY, PersistentDataType.INTEGER, realMaxDurability - finalDamage);
            double realDamageToSet = (double) finalDamage / (double) realMaxDurability * toRepair.getType().getMaxDurability();
            damageable.setDamage((int) realDamageToSet);
        }

        toRepair.setItemMeta(damageable);
        event.setCancelled(true);
        event.getCursor().setAmount(event.getCursor().getAmount() - 1);

    }

}
