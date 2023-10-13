package io.th0rgal.oraxen.mechanics.provided.misc.armor_effects;

import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ArmorEffectsListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemWorn(ArmorEquipEvent event) {
        ArmorEffectsMechanic.addEffects(event.getPlayer());
    }

}
