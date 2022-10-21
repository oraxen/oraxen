package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorldEditListener implements Listener {

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        List<String> args = Arrays.stream(event.getBuffer().split(" ")).toList();
        if (!event.getBuffer().startsWith("//") || args.isEmpty()) return;

        List<String> ids = oraxenBlockIDs.stream().filter(id ->
                ("oraxen:" + id).startsWith(args.get(args.size() - 1)))
                .map("oraxen:"::concat).collect(Collectors.toList());
        ids.addAll(event.getCompletions());
        event.setCompletions(ids);
    }

    private final List<String> oraxenBlockIDs =
            OraxenItems.getEntries().stream().map(entry -> entry.getKey().toLowerCase()).filter(this::isOraxenBlock).toList();

    private boolean isOraxenBlock(String id) {
        NoteBlockMechanic nMechanic = (NoteBlockMechanic) MechanicsManager.getMechanicFactory("noteblock").getMechanic(id);
        StringBlockMechanic sMechanic = (StringBlockMechanic) MechanicsManager.getMechanicFactory("stringblock").getMechanic(id);
        return nMechanic != null || sMechanic != null;
    }
}
