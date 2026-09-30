package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.MinecraftVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemComponentsTest {

    @Test
    void mapsLegacySwingAnimationTo26_3Components() {
        assertEquals(List.of("swing_animation"),
                ItemComponents.resolveGenericComponentKeys("swing_animation", new MinecraftVersion("26.2")));
        assertEquals(List.of("attack_animation", "interact_animation"),
                ItemComponents.resolveGenericComponentKeys("swing_animation", new MinecraftVersion("26.3")));
        assertEquals(List.of("attack_animation", "interact_animation"),
                ItemComponents.resolveGenericComponentKeys("SWING_ANIMATION", new MinecraftVersion("1.26.3")));
    }

    @Test
    void keeps26_3ComponentNames() {
        MinecraftVersion version = new MinecraftVersion("26.3");
        assertEquals(List.of("compostable"), ItemComponents.resolveGenericComponentKeys("compostable", version));
        assertEquals(List.of("cooking_fuel"), ItemComponents.resolveGenericComponentKeys("cooking_fuel", version));
        assertEquals(List.of("brewing_fuel"), ItemComponents.resolveGenericComponentKeys("brewing_fuel", version));
        assertEquals(List.of("attack_animation"), ItemComponents.resolveGenericComponentKeys("attack_animation", version));
        assertEquals(List.of("interact_animation"), ItemComponents.resolveGenericComponentKeys("interact_animation", version));
    }
}
