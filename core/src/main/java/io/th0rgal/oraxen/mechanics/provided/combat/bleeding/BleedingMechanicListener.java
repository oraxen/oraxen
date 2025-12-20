package io.th0rgal.oraxen.mechanics.provided.combat.bleeding;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BleedingMechanicListener implements Listener {

    private final MechanicFactory factory;
    // Use thread-safe map for Folia compatibility (concurrent region thread access)
    private final Map<UUID, SchedulerUtil.ScheduledTask> bleedingTasks = new ConcurrentHashMap<>();

    public BleedingMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager))
            return;
        if (!(event.getEntity() instanceof LivingEntity victim))
            return;
        if (!ProtectionLib.canInteract(damager, event.getEntity().getLocation()))
            return;

        String itemID = OraxenItems.getIdByItem(damager.getInventory().getItemInMainHand());
        if (!OraxenItems.exists(itemID))
            return;
        BleedingMechanic mechanic = (BleedingMechanic) factory.getMechanic(itemID);
        if (mechanic == null)
            return;

        if (Math.random() > mechanic.getChance())
            return;

        applyBleeding(victim, mechanic);
    }

    private void applyBleeding(LivingEntity victim, BleedingMechanic mechanic) {
        UUID victimId = victim.getUniqueId();

        SchedulerUtil.ScheduledTask existingTask = bleedingTasks.remove(victimId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        final int[] ticksRemaining = {mechanic.getDuration()};

        SchedulerUtil.ScheduledTask task = SchedulerUtil.runForEntityTimer(victim, 0L, mechanic.getTickInterval(), () -> {
            if (!victim.isValid() || victim.isDead() || ticksRemaining[0] <= 0) {
                SchedulerUtil.ScheduledTask taskToCancel = bleedingTasks.remove(victimId);
                if (taskToCancel != null) {
                    taskToCancel.cancel();
                }
                return;
            }

            victim.damage(mechanic.getDamagePerTick());
            victim.getWorld().spawnParticle(
                    Particle.BLOCK,
                    victim.getLocation().add(0, 1, 0),
                    10,
                    0.3, 0.5, 0.3,
                    0.1,
                    org.bukkit.Material.REDSTONE_BLOCK.createBlockData()
            );

            ticksRemaining[0] -= mechanic.getTickInterval();
        }, () -> bleedingTasks.remove(victimId));

        // Only store task if it was successfully scheduled (can be null on Folia if entity is retired)
        if (task != null) {
            bleedingTasks.put(victimId, task);
        }
    }

}