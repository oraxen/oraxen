package io.th0rgal.oraxen.mechanics.provided.combat.spear;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mechanic that enables a spear-style lunge attack with model swapping.
 * The spear starts in an inactive (vertical) pose and transitions to an active
 * (horizontal) pose while charging. On release, the player lunges forward.
 * <p>
 * This mechanic integrates with Oraxen's resource pack generation system.
 * The base model is derived from the item's Pack.model setting.
 * The active model is specified via the "active_model" config and a
 * corresponding
 * item model definition is automatically generated during pack creation.
 */
public class SpearLungeMechanic extends Mechanic {

    // Model paths (used for pack generation)
    private final String activeModelPath;
    private final String[] intermediateModelPaths;

    // The NamespacedKey for the active model (oraxen:<itemId>_active)
    private final NamespacedKey activeItemModelKey;

    // Gameplay settings
    private final int chargeTicks;
    private final double lungeVelocity;
    private final double maxRange;
    private final double damage;
    private final double minDamage;
    private final double knockback;
    private final double hitboxRadius;
    private final int smoothFrames;
    private final NamespacedKey[] intermediateModelKeys;

    // Visual/Audio feedback
    private final boolean enableParticles;
    private final Particle chargeParticle;
    private final Particle lungeParticle;
    private final Particle hitParticle;
    private final boolean enableSounds;
    private final Sound chargeSound;
    private final Sound lungeSound;
    private final Sound hitSound;

    // Gameplay modifiers
    private final int maxTargets;
    private final double minChargePercent;

    // Movement and timing modifiers
    private final double chargeSlowdown; // 0.0 to 1.0, percentage of speed reduction while charging
    private final int maxHoldTicks; // Max time to hold before auto-cancel (without attack)

    public SpearLungeMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);

        // Parse active model path (e.g., "default/lotrpikeactive")
        // This follows the same format as Pack.model
        this.activeModelPath = section.getString("active_model", null);

        // Generate the NamespacedKey for the active model
        // Uses oraxen namespace with format: <itemId>_active
        String activeModelId = getItemID() + "_active";
        this.activeItemModelKey = new NamespacedKey(OraxenPlugin.get(), activeModelId);

        this.chargeTicks = section.getInt("charge_ticks", 12);
        this.lungeVelocity = section.getDouble("lunge_velocity", 0.6);
        this.maxRange = section.getDouble("max_range", 3.5);
        this.damage = section.getDouble("damage", 6.0);
        // Minimum lunge damage (applied at 0% charge; still requires min_charge_percent to attack)
        this.minDamage = Math.max(0.0, Math.min(section.getDouble("min_damage", 0.0), this.damage));
        this.knockback = section.getDouble("knockback", 0.5);
        // Ray trace "width" for hit detection. Higher values make it easier to hit targets off-center.
        this.hitboxRadius = Math.max(0.0, Math.min(section.getDouble("hitbox_radius", 0.5), 5.0));
        this.smoothFrames = section.getInt("smooth_frames", 0);

        // Gameplay modifiers
        this.maxTargets = section.getInt("max_targets", 1);
        this.minChargePercent = section.getDouble("min_charge_percent", 0.3);

        // Movement and timing modifiers
        // charge_slowdown: 0.0 = no slowdown, 0.5 = 50% slower, 1.0 = cannot move
        this.chargeSlowdown = Math.max(0.0, Math.min(1.0, section.getDouble("charge_slowdown", 0.4)));
        // max_hold_ticks: how long player can hold before auto-cancel (default 3
        // seconds = 60 ticks)
        this.maxHoldTicks = section.getInt("max_hold_ticks", 60);

        // Parse intermediate models for smooth animation
        if (smoothFrames > 0 && section.isList("intermediate_models")) {
            var modelList = section.getStringList("intermediate_models");
            int frameCount = Math.min(modelList.size(), smoothFrames);
            intermediateModelPaths = new String[frameCount];
            intermediateModelKeys = new NamespacedKey[frameCount];
            for (int i = 0; i < frameCount; i++) {
                intermediateModelPaths[i] = modelList.get(i);
                // Intermediate models use oraxen namespace with format: <itemId>_frame<n>
                String intermediateId = getItemID() + "_frame" + i;
                intermediateModelKeys[i] = new NamespacedKey(OraxenPlugin.get(), intermediateId);
            }
        } else {
            intermediateModelPaths = new String[0];
            intermediateModelKeys = new NamespacedKey[0];
        }

        // Particle settings
        ConfigurationSection particleSection = section.getConfigurationSection("particles");
        if (particleSection != null && particleSection.getBoolean("enabled", true)) {
            this.enableParticles = true;
            this.chargeParticle = parseParticle(particleSection.getString("charge", "CRIT"));
            this.lungeParticle = parseParticle(particleSection.getString("lunge", "SWEEP_ATTACK"));
            this.hitParticle = parseParticle(particleSection.getString("hit", "DAMAGE_INDICATOR"));
        } else {
            this.enableParticles = section.getBoolean("enable_particles", true);
            this.chargeParticle = Particle.CRIT;
            this.lungeParticle = Particle.SWEEP_ATTACK;
            this.hitParticle = Particle.DAMAGE_INDICATOR;
        }

        // Sound settings
        ConfigurationSection soundSection = section.getConfigurationSection("sounds");
        if (soundSection != null && soundSection.getBoolean("enabled", true)) {
            this.enableSounds = true;
            this.chargeSound = parseSound(soundSection.getString("charge", "ITEM_TRIDENT_RIPTIDE_1"));
            this.lungeSound = parseSound(soundSection.getString("lunge", "ENTITY_PLAYER_ATTACK_SWEEP"));
            this.hitSound = parseSound(soundSection.getString("hit", "ENTITY_PLAYER_ATTACK_STRONG"));
        } else {
            this.enableSounds = section.getBoolean("enable_sounds", true);
            this.chargeSound = Sound.ITEM_TRIDENT_RIPTIDE_1;
            this.lungeSound = Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            this.hitSound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
        }
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid particle name, use fallback silently
            return Particle.CRIT;
        } catch (IncompatibleClassChangeError e) {
            // Handle version compatibility issues with Particle.valueOf()
            return Particle.CRIT;
        } catch (Exception e) {
            // Unexpected error - log with stack trace for diagnostics
            e.printStackTrace();
            return Particle.CRIT;
        }
    }

    private Sound parseSound(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Sound sound = Registry.SOUNDS.get(key);
            return sound != null ? sound : Sound.ENTITY_PLAYER_ATTACK_SWEEP;
        } catch (IllegalArgumentException e) {
            // Invalid sound name, use fallback silently
            return Sound.ENTITY_PLAYER_ATTACK_SWEEP;
        } catch (IncompatibleClassChangeError e) {
            // Handle version compatibility issues with Sound registry access
            return Sound.ENTITY_PLAYER_ATTACK_SWEEP;
        } catch (Exception e) {
            // Unexpected error - log with stack trace for diagnostics
            e.printStackTrace();
            return Sound.ENTITY_PLAYER_ATTACK_SWEEP;
        }
    }

    /**
     * Returns true if this mechanic has an active model configured.
     */
    public boolean hasActiveModel() {
        return activeModelPath != null && !activeModelPath.isEmpty();
    }

    /**
     * Gets the model path for the active state (e.g., "default/lotrpikeactive").
     * This is used during pack generation to create the item model definition.
     */
    @Nullable
    public String getActiveModelPath() {
        return activeModelPath;
    }

    /**
     * Gets the NamespacedKey for the active item model.
     * This is what should be set on the item's item_model component when switching.
     * Format: oraxen:<itemId>_active
     */
    @NotNull
    public NamespacedKey getActiveItemModelKey() {
        return activeItemModelKey;
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public double getLungeVelocity() {
        return lungeVelocity;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public double getDamage() {
        return damage;
    }

    public double getMinDamage() {
        return minDamage;
    }

    public double getKnockback() {
        return knockback;
    }

    public double getHitboxRadius() {
        return hitboxRadius;
    }

    public int getSmoothFrames() {
        return smoothFrames;
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    public double getMinChargePercent() {
        return minChargePercent;
    }

    /**
     * Returns the movement slowdown factor while charging (0.0 to 1.0).
     * 0.0 means no slowdown, 0.5 means 50% speed reduction, 1.0 means cannot move.
     */
    public double getChargeSlowdown() {
        return chargeSlowdown;
    }

    /**
     * Returns the maximum number of ticks the player can hold the charge
     * before it auto-cancels (without performing an attack).
     * Default is 60 ticks (3 seconds).
     */
    public int getMaxHoldTicks() {
        return maxHoldTicks;
    }

    @Nullable
    public NamespacedKey getIntermediateModelKey(int frame) {
        if (frame < 0 || frame >= intermediateModelKeys.length) {
            return null;
        }
        return intermediateModelKeys[frame];
    }

    @Nullable
    public String getIntermediateModelPath(int frame) {
        if (frame < 0 || frame >= intermediateModelPaths.length) {
            return null;
        }
        return intermediateModelPaths[frame];
    }

    public int getIntermediateModelCount() {
        return intermediateModelPaths.length;
    }

    public boolean hasSmoothAnimation() {
        return smoothFrames > 0 && intermediateModelKeys.length > 0;
    }

    public boolean hasParticles() {
        return enableParticles;
    }

    public Particle getChargeParticle() {
        return chargeParticle;
    }

    public Particle getLungeParticle() {
        return lungeParticle;
    }

    public Particle getHitParticle() {
        return hitParticle;
    }

    public boolean hasSounds() {
        return enableSounds;
    }

    public Sound getChargeSound() {
        return chargeSound;
    }

    public Sound getLungeSound() {
        return lungeSound;
    }

    public Sound getHitSound() {
        return hitSound;
    }
}
