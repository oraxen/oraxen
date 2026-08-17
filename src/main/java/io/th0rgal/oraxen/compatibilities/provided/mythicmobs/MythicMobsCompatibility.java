package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.utils.numbers.RandomDouble;
import io.lumine.mythic.core.drops.Drop;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.MythicUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class MythicMobsCompatibility extends CompatibilityProvider<MythicBukkit> {

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (!event.getDropName().equalsIgnoreCase("oraxen")) return;

        String line = event.getContainer().getLine();
        String[] lines = line.trim().split("\\s+");
        String itemId = MythicMobsDropParser.getItemId(lines);
        String amountRange = Arrays.stream(lines).filter(s -> s.contains("-")).findFirst().orElse("1-1");
        ItemBuilder builder = OraxenItems.getItemById(itemId);
        if (builder == null) {
            Logs.logWarning("Skipping MythicMobs drop line with unknown Oraxen item '" + itemId + "': " + line);
            return;
        }

        ItemStack oraxenItem = builder.build();
        Drop drop = MythicUtil.getOraxenDrop(line, event.getConfig(), oraxenItem, new RandomDouble(amountRange));
        event.register(drop);
    }
}
