package io.th0rgal.oraxen.compatibilities.provided.worldedit;

import com.sk89q.worldedit.event.platform.CommandSuggestionEvent;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.event.Listener;

public class WorldEditListener implements Listener {

    @Subscribe
    public void onTabComplete(CommandSuggestionEvent event) {
        String arg = event.getArguments().split(" ")[event.getArguments().split(" ").length - 1].toLowerCase();
        OraxenItems.getEntries().stream().map(entry -> entry.getKey().toLowerCase())
                .filter(id -> id.startsWith(arg))
                .forEach(id -> event.getSuggestions().add(event.getSuggestions().size() - 1, Substring.from(id, 0)));
    }
}
