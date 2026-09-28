package io.th0rgal.oraxen.items;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Applies the 1.21.4+ dropped-item outline on the item's owning region. */
public final class GlowingItemListener implements Listener {

    private static final String TEAM_PREFIX = "orx_glow_";
    private final Plugin plugin;
    private final Map<UUID, NamedTextColor> glowingItems = new ConcurrentHashMap<>();

    public GlowingItemListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (!VersionUtil.atOrAbove("1.21.4") || !(event.getEntity() instanceof Item item)) return;

        ItemBuilder builder = OraxenItems.getItemById(OraxenItems.getIdByItem(item.getItemStack()));
        NamedTextColor color = builder == null || !builder.hasOraxenMeta()
                ? null : builder.getOraxenMeta().getGlowing();
        if (color == null) return;

        item.setGlowing(true);
        glowingItems.put(item.getUniqueId(), color);
        String entry = item.getUniqueId().toString();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> addToTeam(entry, color));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (!VersionUtil.atOrAbove("1.21.4") || !(event.getEntity() instanceof Item item)) return;

        NamedTextColor color = glowingItems.remove(item.getUniqueId());
        if (color == null) return;

        String entry = item.getUniqueId().toString();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> removeFromTeam(entry, color));
    }

    private static void addToTeam(String entry, NamedTextColor color) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String name = teamName(color);
        Team team = scoreboard.getTeam(name);
        if (team == null) team = scoreboard.registerNewTeam(name);
        team.color(color);
        team.addEntry(entry);
    }

    private static void removeFromTeam(String entry, NamedTextColor color) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName(color));
        if (team == null) return;
        team.removeEntry(entry);
        if (team.getEntries().isEmpty()) team.unregister();
    }

    private static String teamName(NamedTextColor color) {
        return TEAM_PREFIX + Integer.toHexString(color.value());
    }
}
