package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike.KnockbackStrikeMechanic;
import io.th0rgal.oraxen.utils.wrappers.ParticleWrapper;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockbackStrikeMechanicTest extends MechanicTestSupport {

    @Test
    void readsSettingsAndTracksRequiredHits() {
        KnockbackStrikeMechanic mechanic = new KnockbackStrikeMechanic(mechanicFactory(), mechanicSection("knockbackstrike",
                "required_hits", 2,
                "knockback_horizontal", 3.0,
                "knockback_vertical", 0.75,
                "reset_time", 100,
                "show_counter", false,
                "play_sound", false,
                "particle", java.util.Map.of("type", "FLAME", "count", 4, "spread", 0.2)));

        assertEquals(2, mechanic.getRequiredHits());
        assertEquals(3.0, mechanic.getKnockbackHorizontal());
        assertEquals(0.75, mechanic.getKnockbackVertical());
        assertEquals(Particle.FLAME, mechanic.getParticleType());
        assertEquals(4, mechanic.getParticleCount());
        assertEquals(0.2, mechanic.getParticleSpread());
        assertFalse(mechanic.shouldPlaySound());
        assertFalse(mechanic.shouldShowCounter());

        UUID player = UUID.randomUUID();
        assertFalse(mechanic.incrementHitAndCheck(player));
        assertEquals(1, mechanic.getCurrentHitCount(player));
        assertTrue(mechanic.incrementHitAndCheck(player));
        assertEquals(0, mechanic.getCurrentHitCount(player));
    }

    @Test
    void resolvesSoundKeysThroughRegistry() {
        java.util.Map<String, NamespacedKey> configuredSounds = java.util.Map.of(
                "ENTITY_PLAYER_ATTACK_STRONG", NamespacedKey.minecraft("entity.player.attack.strong"),
                "entity.player.attack.strong", NamespacedKey.minecraft("entity.player.attack.strong"),
                "minecraft:entity.player.attack.strong", NamespacedKey.minecraft("entity.player.attack.strong"),
                "BLOCK_NOTE_BLOCK_HARP", NamespacedKey.minecraft("block.note_block.harp"));

        configuredSounds.forEach((configuredName, expectedKey) -> {
            assertNotNull(Registry.SOUNDS.get(expectedKey));
            KnockbackStrikeMechanic mechanic = new KnockbackStrikeMechanic(mechanicFactory(),
                    mechanicSection("knockbackstrike", "sound_type", configuredName));

            assertNotNull(mechanic.getSoundType(), "sound type for " + configuredName);
            assertEquals(expectedKey, Registry.SOUNDS.getKey(mechanic.getSoundType()),
                    "sound key for " + configuredName);
        });
    }

    @Test
    void resolvesRenamedParticlesThroughWrapper() {
        java.util.Map<String, Particle> expected = new java.util.LinkedHashMap<>();
        expected.put("BLOCK", ParticleWrapper.BLOCK);
        expected.put("BLOCK_CRACK", ParticleWrapper.BLOCK);
        expected.put("ITEM", ParticleWrapper.ITEM);
        expected.put("ITEM_CRACK", ParticleWrapper.ITEM);
        expected.put("REDSTONE", ParticleWrapper.DUST);
        expected.put("DUST", ParticleWrapper.DUST);
        expected.put("DUST_COLOR_TRANSITION", ParticleWrapper.DUST_COLOR_TRANSITION);

        expected.forEach((configuredName, particle) -> {
            KnockbackStrikeMechanic mechanic = new KnockbackStrikeMechanic(mechanicFactory(),
                    mechanicSection("knockbackstrike",
                            "play_sound", false,
                            "particle", java.util.Map.of("type", configuredName)));

            assertEquals(particle, mechanic.getParticleType(), "particle type for " + configuredName);
        });
    }
}
