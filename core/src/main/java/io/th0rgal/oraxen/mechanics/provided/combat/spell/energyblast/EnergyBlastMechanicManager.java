package io.th0rgal.oraxen.mechanics.provided.combat.spell.energyblast;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.VectorUtils;
import io.th0rgal.oraxen.utils.timers.Timer;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EnergyBlastMechanicManager implements Listener {

    private final MechanicFactory factory;

    public EnergyBlastMechanicManager(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        EnergyBlastMechanic mechanic = (EnergyBlastMechanic) factory.getMechanic(item);
        Block block = event.getClickedBlock();
        Location location = block != null ? block.getLocation() : player.getLocation();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (BlockHelpers.isInteractable(block) && event.useInteractedBlock() == Event.Result.ALLOW) return;
        if (!ProtectionLib.canUse(player, location)) return;
        if (factory.isNotImplementedIn(itemID)) return;
        if (mechanic == null) return;

        Timer playerTimer = mechanic.getTimer(player);

        if (!playerTimer.isFinished()) {
            mechanic.getTimer(player).sendToPlayer(player);
            return;
        }

        playerTimer.reset();

        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection();
        direction.normalize();
        direction.multiply(0.1);
        Location destination = origin.clone().add(direction);
        for (int i = 0; i < mechanic.getLength() * 10; i++) {
            Location loc = destination.add(direction);
            spawnParticle(loc.getWorld(), loc, mechanic);
        }
        mechanic.removeCharge(item);
        playEffect(player, mechanic);
    }


    private void playEffect(Player player, EnergyBlastMechanic mechanic) {
        new BukkitRunnable() {
            final Vector dir = player.getLocation().getDirection().normalize();
            static final int circlePoints = 360;
            double radius = 2;
            final Location playerLoc = player.getEyeLocation();
            final double pitch = (playerLoc.getPitch() + 90.0F) * 0.017453292F;
            final double yaw = -playerLoc.getYaw() * 0.017453292F;
            final double increment = (2 * Math.PI) / circlePoints;
            double circlePointOffset = 0;
            int beamLength = mechanic.getLength() * 2;
            final double radiusShrinkage = radius / ((beamLength + 2) / 2.0);

            @Override
            public void run() {
                beamLength--;
                if (beamLength < 1) {
                    this.cancel();
                    return;
                }
                for (int i = 0; i < circlePoints; i++) {
                    double angle = i * increment + circlePointOffset;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Vector vec = new Vector(x, 0, z);
                    VectorUtils.rotateAroundAxisX(vec, pitch);
                    VectorUtils.rotateAroundAxisY(vec, yaw);
                    playerLoc.add(vec);
                    spawnParticle(playerLoc.getWorld(), playerLoc, mechanic);
                    playerLoc.subtract(vec);
                }

                circlePointOffset += increment / 3;
                if (circlePointOffset >= increment) {
                    circlePointOffset = 0;
                }

                radius -= radiusShrinkage;
                if (radius < 0) {
                    spawnParticle(playerLoc.getWorld(), playerLoc, mechanic, 1000, 0.3, 0.3, 0.3, 0.3);
                    for (Entity entity : playerLoc.getWorld().getNearbyEntities(playerLoc, 0.5, 0.5, 0.5))
                        if (entity instanceof LivingEntity livingEntity && entity != player) {
                            EntityDamageByEntityEvent event = EventUtils.EntityDamageByEntityEvent(player, entity, EntityDamageEvent.DamageCause.MAGIC, DamageType.MAGIC, mechanic.getDamage() * 3.0);
                            if (entity.isDead() || EventUtils.callEvent(event)) continue;
                            entity.setLastDamageCause(event);
                            livingEntity.damage(mechanic.getDamage() * 3.0, player);
                        }
                    this.cancel();
                    return;
                }

                playerLoc.add(dir);
                for (Entity entity : playerLoc.getWorld().getNearbyEntities(playerLoc, radius, radius, radius))
                    if (entity instanceof LivingEntity livingEntity && entity != player) {
                        EntityDamageByEntityEvent event = EventUtils.EntityDamageByEntityEvent(player, entity, EntityDamageEvent.DamageCause.MAGIC, DamageType.MAGIC, mechanic.getDamage());
                        if (livingEntity.isDead() || !EventUtils.callEvent(event)) continue;
                        livingEntity.setLastDamageCause(event);
                        livingEntity.damage(mechanic.getDamage(), player);
                    }

            }
        }.runTaskTimer(OraxenPlugin.get(), 0, 1);
    }

    private void spawnParticle(World world, Location location, EnergyBlastMechanic mechanic) {
        if (mechanic.getParticle() == Particle.REDSTONE)
            world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0, mechanic.getParticleColor());
        else
            world.spawnParticle(mechanic.getParticle(), location, 1, 0, 0, 0, 0);
    }

    private void spawnParticle(World world, Location location, EnergyBlastMechanic mechanic, int amount, double offsetX,
                               double offsetY, double offsetZ, double extra) {
        if (mechanic.getParticle() == Particle.REDSTONE)
            world
                    .spawnParticle(Particle.REDSTONE, location, amount, offsetX, offsetY, offsetZ, extra,
                            mechanic.getParticleColor());
        else
            world.spawnParticle(mechanic.getParticle(), location, amount, offsetX, offsetY, offsetZ, extra);
    }

}
