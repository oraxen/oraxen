package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.utils.numbers.RandomDouble;
import io.lumine.mythic.core.drops.Drop;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.utils.MythicUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class MythicMobsCompatibility extends CompatibilityProvider<MythicBukkit> {

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (!event.getDropName().equalsIgnoreCase("oraxen")) return;

        String line = event.getContainer().getLine();
        String[] lines = line.split(" ");
        String itemId = lines.length == 4 ? lines[1] : lines.length == 3 ? lines[2] : "";
        String amountRange = Arrays.stream(lines).filter(s -> s.contains("-")).findFirst().orElse("1-1");
        ItemStack oraxenItem = OraxenItems.getItemById(itemId).build();
        if (oraxenItem == null) return;

        Drop drop = MythicUtil.getOraxenDrop(line, event.getConfig(), oraxenItem, new RandomDouble(amountRange));
        event.register(drop);
    }
}
