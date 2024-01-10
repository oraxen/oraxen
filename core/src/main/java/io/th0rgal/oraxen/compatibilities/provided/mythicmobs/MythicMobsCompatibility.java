package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.core.drops.Drop;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.utils.MythicUtil;
import org.bukkit.event.EventHandler;

public class MythicMobsCompatibility extends CompatibilityProvider<MythicBukkit> {

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (!event.getDropName().equalsIgnoreCase("oraxen")) return;

        String line = event.getContainer().getLine();
        String[] lines = line.split(" ");
        String itemId = lines.length == 4 ? lines[1] : lines.length == 3 ? lines[2] : "";
        if (!OraxenItems.exists(itemId)) return;

        // MythicMobs 5.6.0 SNAPSHOT changed this functionality
        // This is a workaround to support both old and new moving forward
        Drop drop = MythicUtil.getOraxenDrop(line, event.getConfig(), itemId);
        event.register(drop);
    }
}
