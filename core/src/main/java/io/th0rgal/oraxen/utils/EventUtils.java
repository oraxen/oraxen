package io.th0rgal.oraxen.utils;

import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EventUtils {

    /**
     * Calls the event and tests if cancelled.
     *
     * @return false if event was cancelled, if cancellable. otherwise true.
     */
    public static boolean callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
        if (event instanceof Cancellable cancellable) return !cancellable.isCancelled();
        else return true;
    }

    /** In a recent build of Spigot 1.20.4, they removed undeprecated constructors for EntityDamageByEntityEvent here. This method aims to call the event with backwards compatibility */
    public static EntityDamageByEntityEvent EntityDamageByEntityEvent(Entity damager, Entity entity, EntityDamageEvent.DamageCause cause, DamageType damageType, double damage) {
        try {
            // Old constructor
            return EntityDamageByEntityEvent.class.getConstructor(Entity.class, Entity.class, EntityDamageEvent.DamageCause.class, double.class).newInstance(damager, entity, cause, damage);
        } catch (Exception e) {
            // New constructor
            return new EntityDamageByEntityEvent(damager, entity, cause, DamageSource.builder(damageType).build(), damage);
        }
    }
}
