package io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class KnockbackStrikeMechanicListener implements Listener {

    private final MechanicFactory factory;

    public KnockbackStrikeMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only if player is attacking
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        
        // Entity must be LivingEntity
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        // Protection check
        if (!ProtectionLib.canInteract(attacker, victim.getLocation())) return;

        // Player's held item
        ItemStack item = attacker.getInventory().getItemInMainHand();
        String itemID = OraxenItems.getIdByItem(item);

        // Mechanic check
        if (factory.isNotImplementedIn(itemID)) return;
        
        Mechanic mechanicBase = factory.getMechanic(itemID);
        if (mechanicBase == null) return;
        if (!(mechanicBase instanceof KnockbackStrikeMechanic)) return;
        
        KnockbackStrikeMechanic mechanic = (KnockbackStrikeMechanic) mechanicBase;

        // Increment hit count and check
        boolean shouldKnockback = mechanic.incrementHitAndCheck(attacker.getUniqueId());

        if (shouldKnockback) {
            // Required hits reached, apply knockback
            applyKnockback(attacker, victim, mechanic);
        } else {
            // Not enough hits yet, show counter if show_counter is true
            if (mechanic.shouldShowCounter()) {
                mechanic.showHitCounter(attacker);
            }
        }
    }

    private void applyKnockback(Player attacker, LivingEntity victim, KnockbackStrikeMechanic mechanic) {
        Location victimLoc = victim.getLocation();
        Location attackerLoc = attacker.getLocation();

        // Get victim position (only horizontal - zero Y axis)
        Vector direction = victimLoc.toVector().subtract(attackerLoc.toVector());
        direction.setY(0); // Zero Y axis (only horizontal direction)
        
        // Check if vector is zero before normalizing (prevents NaN)
        if (direction.lengthSquared() == 0) {
            // Players are at same X-Z position, use attacker's facing direction
            direction = attackerLoc.getDirection().setY(0);
            if (direction.lengthSquared() == 0) {
                // Last resort: use X axis direction
                direction = new Vector(1, 0, 0);
            }
        }
        
        direction.normalize(); // Normalize
        
        // Apply horizontal knockback (X and Z axes)
        direction.multiply(mechanic.getKnockbackHorizontal());
        
        // Apply vertical knockback SEPARATELY to Y axis
        direction.setY(mechanic.getKnockbackVertical());

        // Apply knockback
        victim.setVelocity(direction);

        // Spawn particle effect at victim location
        spawnParticles(victimLoc, mechanic);

        // Play sound at victim location
        if (mechanic.shouldPlaySound()) {
            World world = victimLoc.getWorld();
            if (world != null) {
                try {
                    world.playSound(
                        victimLoc,
                        mechanic.getSoundType(),
                        mechanic.getSoundVolume(),
                        mechanic.getSoundPitch()
                    );
                } catch (Exception e) {
                    // Continue silently if sound fails
                }
            }
        }
    }

    private void spawnParticles(Location location, KnockbackStrikeMechanic mechanic) {
        World world = location.getWorld();
        if (world == null) return;
        
        try {
            // Adjust location upward for better visibility
            Location particleLoc = location.clone().add(0, 1.0, 0);
            
            Particle particleType = mechanic.getParticleType();
            int count = mechanic.getParticleCount();
            double spread = mechanic.getParticleSpread();
            
            // Special particles requiring extra parameters
            if (particleType == Particle.DUST) {
                // DUST particle - requires Color
                Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(255, 0, 0), // Red
                    1.0f
                );
                world.spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    dustOptions
                );
            } else if (particleType == Particle.DUST_COLOR_TRANSITION) {
                // Color transition dust - requires two colors
                Particle.DustTransition transition = new Particle.DustTransition(
                    Color.fromRGB(255, 0, 0),   // Start: Red
                    Color.fromRGB(255, 255, 0), // End: Yellow
                    1.0f
                );
                world.spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    transition
                );
            } else if (particleType == Particle.BLOCK || particleType == Particle.FALLING_DUST) {
                // Block particles - require BlockData (using Stone)
                Material blockMat = Material.STONE;
                BlockData blockData = blockMat.createBlockData();
                world.spawnParticle(
                    particleType,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    blockData
                );
            } else if (particleType == Particle.ITEM) {
                // Item particle - requires ItemStack
                ItemStack itemStack = new ItemStack(Material.DIAMOND);
                world.spawnParticle(
                    Particle.ITEM,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    itemStack
                );
            } else if (particleType == Particle.VIBRATION) {
                // Vibration - requires PositionSource (skip, too complex)
                // Can't spawn this particle normally, use another particle
                world.spawnParticle(
                    Particle.GLOW,
                    particleLoc,
                    count,
                    spread, spread, spread
                );
            } else if (particleType == Particle.SCULK_CHARGE) {
                // Sculk charge - requires Roll parameter
                world.spawnParticle(
                    Particle.SCULK_CHARGE,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    0.5f // Roll value
                );
            } else if (particleType == Particle.SHRIEK) {
                // Shriek - requires Delay parameter
                world.spawnParticle(
                    Particle.SHRIEK,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    10 // Delay (ticks)
                );
            } else {
                // Normal particles - no special parameters required
                world.spawnParticle(
                    particleType,
                    particleLoc,
                    count,
                    spread, spread, spread
                );
            }
        } catch (Exception e) {
            // Print error if particle fails to spawn
            System.err.println("[Oraxen] Failed to spawn particle " + mechanic.getParticleType() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
