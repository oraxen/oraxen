package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitItemStack;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicDropLoadEvent;
import io.lumine.xikage.mythicmobs.drops.droppables.ItemDrop;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

public class MythicMobsCompatibility extends CompatibilityProvider<MythicMobs> {

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (event.getDropName().equalsIgnoreCase("oraxen")) {
            String line = event.getContainer().getLine();
            if (line.split(" ").length == 4 && OraxenItems.exists(line.split(" ")[1])) {
                ItemStack od = OraxenItems.getItemById(line.split(" ")[1]).build();
                ItemDrop itemDrop = new ItemDrop(line, event.getConfig(), new BukkitItemStack(od));
                event.register(itemDrop);
            } else if (line.split(" ").length == 3 && OraxenItems.exists(line.split(" ")[2])) {
                ItemStack od = OraxenItems.getItemById(line.split(" ")[2]).build();
                ItemDrop itemDrop = new ItemDrop(line, event.getConfig(), new BukkitItemStack(od));
                event.register(itemDrop);
            }
        }
    }
}