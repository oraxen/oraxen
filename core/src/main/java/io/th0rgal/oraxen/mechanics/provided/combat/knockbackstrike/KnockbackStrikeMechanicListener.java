package io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Sadece oyuncu vuruyorsa
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        
        // Vurulan entity LivingEntity olmalı
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        // Oyuncunun elindeki item
        ItemStack item = attacker.getInventory().getItemInMainHand();
        String itemID = OraxenItems.getIdByItem(item);

        // Mechanic kontrolü
        if (factory.isNotImplementedIn(itemID)) return;
        
        Mechanic mechanicBase = factory.getMechanic(itemID);
        if (mechanicBase == null) return;
        if (!(mechanicBase instanceof KnockbackStrikeMechanic)) return;
        
        KnockbackStrikeMechanic mechanic = (KnockbackStrikeMechanic) mechanicBase;

        // Hit sayısını artır ve kontrol et
        boolean shouldKnockback = mechanic.incrementHitAndCheck(attacker.getUniqueId());

        if (shouldKnockback) {
            // Gerekli hit sayısına ulaşıldı, knockback uygula
            applyKnockback(attacker, victim, mechanic);
        } else {
            // Henüz yeterli hit yok, sayacı göster (sadece show_counter true ise)
            mechanic.showHitCounter(attacker);
        }
    }

    private void applyKnockback(Player attacker, LivingEntity victim, KnockbackStrikeMechanic mechanic) {
    Location victimLoc = victim.getLocation();
    Location attackerLoc = attacker.getLocation();

    // Kurbanın konumunu al (sadece horizontal - Y eksenini 0 yap)
    Vector direction = victimLoc.toVector().subtract(attackerLoc.toVector());
    direction.setY(0); // Y eksenini sıfırla (sadece yatay yön)
    direction.normalize(); // Normalize et
    
    // Horizontal knockback uygula (X ve Z eksenleri)
    direction.multiply(mechanic.getKnockbackHorizontal());
    
    // Vertical knockback'i AYRI olarak Y eksenine ata
    direction.setY(mechanic.getKnockbackVertical());

    // Knockback uygula
    victim.setVelocity(direction);

    // Particle efekti spawn et - victim lokasyonunda
    spawnParticles(victimLoc, mechanic);

    // Ses çal - victim lokasyonunda
    if (mechanic.shouldPlaySound()) {
        try {
            victimLoc.getWorld().playSound(
                victimLoc,
                mechanic.getSoundType(),
                mechanic.getSoundVolume(),
                mechanic.getSoundPitch()
            );
        } catch (Exception e) {
            // Ses çalmazsa sessizce devam et
        }
    }
}

    private void spawnParticles(Location location, KnockbackStrikeMechanic mechanic) {
        try {
            // Particle spawn için location'ı biraz yukarı al (görünür olsun)
            Location particleLoc = location.clone().add(0, 1.0, 0);
            
            Particle particleType = mechanic.getParticleType();
            int count = mechanic.getParticleCount();
            double spread = mechanic.getParticleSpread();
            
            // Özel durum gerektiren particle'lar
            if (particleType == Particle.DUST) {
                // DUST particle - Color gerektirir
                Particle.DustOptions dustOptions = new Particle.DustOptions(
                    Color.fromRGB(255, 0, 0), // Kırmızı
                    1.0f
                );
                location.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    dustOptions
                );
            } else if (particleType == Particle.DUST_COLOR_TRANSITION) {
                // Renk geçişli toz - İki renk gerektirir
                Particle.DustTransition transition = new Particle.DustTransition(
                    Color.fromRGB(255, 0, 0),   // Başlangıç: Kırmızı
                    Color.fromRGB(255, 255, 0), // Bitiş: Sarı
                    1.0f
                );
                location.getWorld().spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    transition
                );
            } else if (particleType == Particle.BLOCK || particleType == Particle.FALLING_DUST) {
                // Blok particle'ları - BlockData gerektirir (Stone kullan)
                Material blockMat = Material.STONE;
                BlockData blockData = blockMat.createBlockData();
                location.getWorld().spawnParticle(
                    particleType,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    blockData
                );
            } else if (particleType == Particle.ITEM) {
                // Item particle - ItemStack gerektirir
                ItemStack itemStack = new ItemStack(Material.DIAMOND);
                location.getWorld().spawnParticle(
                    Particle.ITEM,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    itemStack
                );
            } else if (particleType == Particle.VIBRATION) {
                // Vibration - PositionSource gerektirir (skip et, karmaşık)
                // Bu particle'ı normal olarak spawn edemeyiz, başka bir particle kullan
                location.getWorld().spawnParticle(
                    Particle.GLOW,
                    particleLoc,
                    count,
                    spread, spread, spread
                );
            } else if (particleType == Particle.SCULK_CHARGE) {
                // Sculk charge - Roll parametresi gerektirir
                location.getWorld().spawnParticle(
                    Particle.SCULK_CHARGE,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    0.5f // Roll değeri
                );
            } else if (particleType == Particle.SHRIEK) {
                // Shriek - Delay parametresi gerektirir
                location.getWorld().spawnParticle(
                    Particle.SHRIEK,
                    particleLoc,
                    count,
                    spread, spread, spread,
                    0.0,
                    10 // Delay (ticks)
                );
            } else {
                // Normal particle'lar - Özel parametre gerektirmez
                location.getWorld().spawnParticle(
                    particleType,
                    particleLoc,
                    count,
                    spread, spread, spread
                );
            }
        } catch (Exception e) {
            // Particle spawn edilemezse hata yazdır
            System.err.println("[Oraxen] Failed to spawn particle " + mechanic.getParticleType() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}