package io.th0rgal.oraxen.nms.handler.java21;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NmsAccessorsTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void registryLookupsResolveIdentifiersFromStrings() throws Exception {
        assertEquals(Optional.of(DataComponents.FOOD), NmsAccessors.dataComponentType("minecraft:food"));
        assertEquals(Optional.of(MobEffects.SPEED.value()), NmsAccessors.mobEffect("minecraft:speed"));
        assertEquals(Optional.of(SoundEvents.TRIDENT_THROW.value()), NmsAccessors.soundEvent("minecraft:item.trident.throw"));
        assertEquals(EntityType.ARMOR_STAND, NmsAccessors.entityType("minecraft:armor_stand"));
        assertEquals(Optional.empty(), NmsAccessors.mobEffect("oraxen:missing"));
    }

    @Test
    void vanillaSoundPathIgnoresCustomNamespaces() throws Exception {
        assertEquals("item.trident.throw", NmsAccessors.vanillaSoundPath(NmsAccessors.createSoundEvent("minecraft:item.trident.throw")));
        assertEquals("", NmsAccessors.vanillaSoundPath(NmsAccessors.createSoundEvent("oraxen:item.trident.throw")));
    }
}
