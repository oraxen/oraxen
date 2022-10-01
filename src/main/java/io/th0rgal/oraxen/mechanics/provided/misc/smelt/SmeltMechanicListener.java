package io.th0rgal.oraxen.mechanics.provided.misc.smelt;

import io.lumine.mythiccrucible.MythicCrucible;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.food.FoodMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.food.FoodMechanicFactory;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        String itemID = OraxenItems.getIdByItem(event.getFuel());
        if (itemID == null || factory.isNotImplementedIn(itemID))
            return;
        SmeltMechanic mechanic = (SmeltMechanic) factory.getMechanic(itemID);
        if (mechanic.getBurnTime() < 0)
            return;
        event.setBurnTime(mechanic.getBurnTime());
        if (!mechanic.isHasReplacement()) return;
        Block furnace = event.getBlock();
        if (furnace.getState() instanceof Furnace) {
            Furnace furnaceData = (Furnace) furnace.getState();
            FurnaceInventory furnaceInventory = furnaceData.getInventory();
            switch(mechanic.getReplacementItemType().toLowerCase()) {
                case "minecraft_item": furnaceInventory.setFuel(new ItemStack(Objects.requireNonNull(Material.getMaterial(mechanic.getReplacementItem()))));
                case "oraxen_item": furnaceInventory.setFuel(OraxenItems.getItemById(mechanic.getReplacementItem()).build());
                case "curcible_item": furnaceInventory.setFuel(MythicCrucible.core().getItemManager().getItemStack(mechanic.getReplacementItem()));
                default:
                    Logs.logError("Invalid replacement item type: " + mechanic.getReplacementItemType());
                    return;
            }
        }
    }
}
