package io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KnockbackStrikeMechanic extends Mechanic {

    private final int requiredHits;
    private final double knockbackHorizontal;
    private final double knockbackVertical;
    private final Particle particleType;
    private final int particleCount;
    private final double particleSpread;
    private final boolean playSound;
    private final Sound soundType;
    private final float soundVolume;
    private final float soundPitch;
    private final int resetTime;
    private final boolean showCounter;

    // Oyuncu başına hit sayacı
    private final Map<UUID, HitData> hitCounters = new HashMap<>();

    public KnockbackStrikeMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        
        this.requiredHits = section.getInt("required_hits", 3);
        this.knockbackHorizontal = section.getDouble("knockback_horizontal", 2.0);
        this.knockbackVertical = section.getDouble("knockback_vertical", 0.5);
        this.resetTime = section.getInt("reset_time", 60);
        this.showCounter = section.getBoolean("show_counter", true);

        // Particle ayarları
        ConfigurationSection particleSection = section.getConfigurationSection("particle");
        if (particleSection != null) {
            String particleName = particleSection.getString("type", "CRIT");
            this.particleType = parseParticle(particleName);
            this.particleCount = particleSection.getInt("count", 20);
            this.particleSpread = particleSection.getDouble("spread", 0.5);
        } else {
            this.particleType = Particle.CRIT;
            this.particleCount = 20;
            this.particleSpread = 0.5;
        }

        // Ses ayarları
        this.playSound = section.getBoolean("play_sound", true);
        String soundName = section.getString("sound_type", "ENTITY_PLAYER_ATTACK_KNOCKBACK");
        this.soundType = parseSound(soundName);
        this.soundVolume = (float) section.getDouble("sound_volume", 1.0);
        this.soundPitch = (float) section.getDouble("sound_pitch", 1.0);
    }

    public int getRequiredHits() {
        return requiredHits;
    }

    public double getKnockbackHorizontal() {
        return knockbackHorizontal;
    }

    public double getKnockbackVertical() {
        return knockbackVertical;
    }

    public Particle getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public double getParticleSpread() {
        return particleSpread;
    }

    public boolean shouldPlaySound() {
        return playSound;
    }

    public Sound getSoundType() {
        return soundType;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public int getResetTime() {
        return resetTime;
    }

    public boolean shouldShowCounter() {
        return showCounter;
    }

    /**
     * Oyuncunun hit sayısını artırır ve gerekli sayıya ulaştıysa true döner
     */
    public boolean incrementHitAndCheck(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        HitData data = hitCounters.get(playerUUID);

        if (data == null || currentTime - data.lastHitTime > (resetTime * 50L)) {
            // İlk hit veya zaman aşımı
            data = new HitData(1, currentTime);
            hitCounters.put(playerUUID, data);
            return false;
        } else {
            // Hit sayısını artır
            data.hitCount++;
            data.lastHitTime = currentTime;

            if (data.hitCount >= requiredHits) {
                // Gerekli hit sayısına ulaşıldı, sayacı sıfırla
                hitCounters.remove(playerUUID);
                return true;
            }
            return false;
        }
    }

    /**
     * Oyuncunun mevcut hit sayısını döner
     */
    public int getCurrentHitCount(UUID playerUUID) {
        HitData data = hitCounters.get(playerUUID);
        if (data == null) return 0;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - data.lastHitTime > (resetTime * 50L)) {
            hitCounters.remove(playerUUID);
            return 0;
        }
        
        return data.hitCount;
    }

    /**
     * Oyuncuya hit sayacını göster (sadece show_counter true ise)
     */
    public void showHitCounter(Player player) {
        // Actionbar mesajı tamamen devre dışı
        return;
    }

    private Particle parseParticle(String particleName) {
        try {
            // DUST ve REDSTONE aynı particle
            if (particleName.equalsIgnoreCase("DUST") || particleName.equalsIgnoreCase("REDSTONE")) {
                return Particle.DUST;
            }
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("[Oraxen] Invalid particle type: " + particleName + ", using CRIT as fallback");
            return Particle.CRIT;
        }
    }

    private Sound parseSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("[Oraxen] Invalid sound type: " + soundName + ", using ENTITY_PLAYER_ATTACK_KNOCKBACK as fallback");
            return Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK;
        }
    }

    /**
     * Hit verilerini tutan iç sınıf
     */
    private static class HitData {
        int hitCount;
        long lastHitTime;

        HitData(int hitCount, long lastHitTime) {
            this.hitCount = hitCount;
            this.lastHitTime = lastHitTime;
        }
    }
}