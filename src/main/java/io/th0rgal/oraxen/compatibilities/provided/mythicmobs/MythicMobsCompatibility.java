package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.core.drops.droppables.ItemDrop;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IDrop;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;

public class MythicMobsCompatibility extends CompatibilityProvider<MythicBukkit> {

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (event.getDropName().equalsIgnoreCase("oraxen")) {
			event.register(new OraxenDropMythic(event.getConfig(), event.getArgument()));
        }
    }
    
    public class OraxenDropMythic implements IDrop {
        private String material;
        public ItemsAdder(MythicLineConfig config, String argument) {
            material = config.getString(new String[] {"item", "key", "id"});
        }
        public AbstractItemStack getDrop(DropMetadata arg) {
            if (OraxenItems.exists(material)) {
                ItemStack od = OraxenItems.getItemById(OraxenItems.exists(material)).build();
                return new BukkitItemStack(od);
            } 
            
            ItemStack item = new ItemStack(Material.STONE);
            return new BukkitItemStack(item);
        }
}

}
