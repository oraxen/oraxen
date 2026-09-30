package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockBreakEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockDamageEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockInteractEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.shapedblock.OraxenShapedBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockInteractEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plugins compiled against earlier releases call {@code getMechanic()} with the concrete mechanic
 * as return type. Each event must keep declaring that method, or those plugins fail with NoSuchMethodError.
 */
class MechanicEventBinaryCompatibilityTest {

    private static final Map<Class<?>, Class<?>> MECHANIC_TYPES = Map.ofEntries(
            Map.entry(OraxenChorusBlockBreakEvent.class, ChorusBlockMechanic.class),
            Map.entry(OraxenChorusBlockDamageEvent.class, ChorusBlockMechanic.class),
            Map.entry(OraxenChorusBlockInteractEvent.class, ChorusBlockMechanic.class),
            Map.entry(OraxenChorusBlockPlaceEvent.class, ChorusBlockMechanic.class),
            Map.entry(OraxenFurnitureBreakEvent.class, FurnitureMechanic.class),
            Map.entry(OraxenFurnitureDamageEvent.class, FurnitureMechanic.class),
            Map.entry(OraxenFurnitureInteractEvent.class, FurnitureMechanic.class),
            Map.entry(OraxenFurniturePlaceEvent.class, FurnitureMechanic.class),
            Map.entry(OraxenNoteBlockBreakEvent.class, NoteBlockMechanic.class),
            Map.entry(OraxenNoteBlockDamageEvent.class, NoteBlockMechanic.class),
            Map.entry(OraxenNoteBlockInteractEvent.class, NoteBlockMechanic.class),
            Map.entry(OraxenNoteBlockPlaceEvent.class, NoteBlockMechanic.class),
            Map.entry(OraxenShapedBlockBreakEvent.class, ShapedBlockMechanic.class),
            Map.entry(OraxenStringBlockBreakEvent.class, StringBlockMechanic.class),
            Map.entry(OraxenStringBlockDamageEvent.class, StringBlockMechanic.class),
            Map.entry(OraxenStringBlockInteractEvent.class, StringBlockMechanic.class),
            Map.entry(OraxenStringBlockPlaceEvent.class, StringBlockMechanic.class)
    );

    @Test
    void eachEventDeclaresItsConcreteMechanicGetter() throws NoSuchMethodException {
        for (Map.Entry<Class<?>, Class<?>> entry : MECHANIC_TYPES.entrySet()) {
            Class<?> declaredType = entry.getKey().getDeclaredMethod("getMechanic").getReturnType();
            assertEquals(entry.getValue(), declaredType, entry.getKey().getSimpleName());
        }
    }
}
