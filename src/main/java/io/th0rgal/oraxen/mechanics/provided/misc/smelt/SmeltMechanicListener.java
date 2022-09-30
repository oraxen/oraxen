package io.th0rgal.oraxen.mechanics.provided.misc.smelt;

import io.lumine.mythiccrucible.MythicCrucible;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.food.FoodMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.food.FoodMechanicFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SmeltMechanicListener implements Listener {
    private final SmeltMechanicFactory factory;

    public SmeltMechanicListener(SmeltMechanicFactory factory) {
        this.factory = factory;
    }

    public void onFurnaceBurn(FurnaceBurnEvent event) {
        String itemID = OraxenItems.getIdByItem(event.getFuel());
        if (itemID == null || factory.isNotImplementedIn(itemID))
            return;
        SmeltMechanic mechanic = (SmeltMechanic) factory.getMechanic(itemID);
        if (mechanic.getBurnTime() < 0)
            return;
        event.setBurnTime(mechanic.getBurnTime());
        if (mechanic.isHasReplacement()) {
            //some hack to get the furnace lol
            Block furnace = event.getBlock();
            if (furnace.getState() instanceof Furnace) {
                Furnace furnaceData = (Furnace) furnace.getState();
                FurnaceInventory furnaceInventory = furnaceData.getInventory();
                if (mechanic.getReplacementItemType().toUpperCase().equals("MINECRAFT_ITEM"))
                    furnaceInventory.setFuel(new ItemStack(Objects.requireNonNull(Material.getMaterial(mechanic.getReplacementItem()))));
                else if (mechanic.getReplacementItemType().toUpperCase().equals("ORAXEN_ITEM"))
                    furnaceInventory.setFuel(OraxenItems.getItemById(mechanic.getReplacementItem()).build());
                else if (mechanic.getReplacementItemType().toUpperCase().equals("CRUCIBLE_ITEM"))
                    furnaceInventory.setFuel(MythicCrucible.core().getItemManager().getItemStack(mechanic.getReplacementItem()));
                else
                    return;
            }
        }
    }
}
