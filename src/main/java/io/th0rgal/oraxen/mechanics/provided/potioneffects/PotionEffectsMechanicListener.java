package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;


public class PotionEffectsMechanicListener implements Listener {

    private final PotionEffectsMechanicFactory factory;

    public PotionEffectsMechanicListener(PotionEffectsMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemWore(ArmorEquipEvent event) {


    }

}
