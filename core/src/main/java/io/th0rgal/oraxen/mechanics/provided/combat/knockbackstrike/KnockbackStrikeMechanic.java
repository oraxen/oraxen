package io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.wrappers.ParticleWrapper;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    // Use thread-safe map for Folia compatibility (concurrent region thread access)
    private final Map<UUID, HitData> hitCounters = new ConcurrentHashMap<>();

    public KnockbackStrikeMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        
        this.requiredHits = section.getInt("required_hits", 3);
        this.knockbackHorizontal = section.getDouble("knockback_horizontal", 2.0);
        this.knockbackVertical = section.getDouble("knockback_vertical", 0.5);
        this.resetTime = section.getInt("reset_time", 60);
        this.showCounter = section.getBoolean("show_counter", true);

        // Particle settings
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

        // Sound settings
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
     * Increments player's hit count and returns true if required hits reached
     * Thread-safe implementation using atomic compute operation for Folia compatibility
     */
    public boolean incrementHitAndCheck(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        
        // Use array to capture result from atomic lambda
        boolean[] shouldTrigger = {false};
        
        hitCounters.compute(playerUUID, (key, data) -> {
            // This entire lambda executes atomically - no race conditions
            
            if (data == null || currentTime - data.lastHitTime > (resetTime * 50L)) {
                // First hit or timeout - reset counter
                if (requiredHits <= 1) {
                    // Trigger immediately if only 1 hit required
                    shouldTrigger[0] = true;
                    return null; // Remove from map
                }
                // Start new counter
                return new HitData(1, currentTime);
            } else {
                // Increment hit count atomically
                int newCount = data.hitCount + 1;
                
                if (newCount >= requiredHits) {
                    // Required hits reached - trigger and reset
                    shouldTrigger[0] = true;
                    return null; // Remove from map
                }
                // Update with new count and time
                return new HitData(newCount, currentTime);
            }
        });
        
        return shouldTrigger[0];
    }

    /**
     * Returns current hit count for player
     * Thread-safe implementation
     */
    public int getCurrentHitCount(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        
        // Use computeIfPresent for atomic read and potential removal
        HitData data = hitCounters.computeIfPresent(playerUUID, (key, existingData) -> {
            if (currentTime - existingData.lastHitTime > (resetTime * 50L)) {
                // Expired - remove from map
                return null;
            }
            // Still valid - keep it
            return existingData;
        });
        
        return data != null ? data.hitCount : 0;
    }

    /**
     * Method for showing hit counter (empty since actionbar is removed)
     */
    public void showHitCounter(Player player) {
        // Actionbar removed, method is empty
        // If you want to show counter in a different way, you can modify this
    }

    private Particle parseParticle(String particleName) {
        try {
            // Handle special particles that have version compatibility issues
            if (particleName.equalsIgnoreCase("DUST") || particleName.equalsIgnoreCase("REDSTONE")) {
                return ParticleWrapper.DUST;
            }
            if (particleName.equalsIgnoreCase("SPLASH") || particleName.equalsIgnoreCase("WATER_SPLASH")) {
                return ParticleWrapper.SPLASH;
            }
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logs.logWarning("Invalid particle type: " + particleName + ", using CRIT as fallback");
            return Particle.CRIT;
        }
    }

    private Sound parseSound(String soundName) {
        try {
            // Try direct valueOf first
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Sound fallback = getSafeFallbackSound();
            Logs.logWarning("Invalid sound type: " + soundName + ", using " + fallback.name() + " as fallback");
            return fallback;
        } catch (IncompatibleClassChangeError e) {
            // Handle version compatibility issues with Sound.valueOf() (includes NoSuchMethodError)
            Logs.logWarning("Sound compatibility issue with " + soundName + ", using fallback sound");
            return getSafeFallbackSound();
        } catch (Exception e) {
            Logs.logError("Unexpected error parsing sound " + soundName + ": " + e.getMessage());
            return getSafeFallbackSound();
        }
    }

    /**
     * Get a safe fallback sound that should exist in most versions
     */
    private Sound getSafeFallbackSound() {
        try {
            return Sound.valueOf("ENTITY_PLAYER_ATTACK_KNOCKBACK");
        } catch (Exception e1) {
            try {
                // Try older sound name
                return Sound.valueOf("ENTITY_PLAYER_ATTACK_STRONG");
            } catch (Exception e2) {
                try {
                    // Try even older sound name
                    return Sound.valueOf("SUCCESSFUL_HIT");
                } catch (Exception e3) {
                    // Last resort - return first available sound
                    Sound[] sounds = Sound.values();
                    if (sounds.length > 0) {
                        return sounds[0];
                    }
                    // This should never happen in a real server environment
                    throw new RuntimeException("No Sound enum values available - server environment is corrupted");
                }
            }
        }
    }

    /**
     * Inner class to hold hit data - immutable for thread safety
     */
    private static class HitData {
        final int hitCount;
        final long lastHitTime;

        HitData(int hitCount, long lastHitTime) {
            this.hitCount = hitCount;
            this.lastHitTime = lastHitTime;
        }
    }
}
