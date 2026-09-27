package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ItemSpawnEvent;

public class InvulnerableItemListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!VersionUtil.atOrAbove("1.21.5")) return;
        Item item = event.getEntity();
        ItemBuilder builder = OraxenItems.getItemById(OraxenItems.getIdByItem(item.getItemStack()));
        if (builder != null && protectsFromFire(builder))
            item.setVisualFire(TriState.FALSE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        ItemBuilder builder = OraxenItems.getItemById(OraxenItems.getIdByItem(item.getItemStack()));
        if (builder != null && builder.isInvulnerableTo(event.getCause())) {
            event.setCancelled(true);
            if (protectsFromFire(builder) && VersionUtil.atOrAbove("1.21.5"))
                item.setVisualFire(TriState.FALSE);
        }
    }

    private boolean protectsFromFire(ItemBuilder builder) {
        return builder.isInvulnerableTo(DamageCause.LAVA)
                || builder.isInvulnerableTo(DamageCause.FIRE)
                || builder.isInvulnerableTo(DamageCause.FIRE_TICK);
    }
}
