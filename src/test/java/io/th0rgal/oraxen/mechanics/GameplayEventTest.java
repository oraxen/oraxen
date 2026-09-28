package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.events.OraxenBreakEvent;
import io.th0rgal.oraxen.api.events.OraxenDamageEvent;
import io.th0rgal.oraxen.api.events.OraxenInteractEvent;
import io.th0rgal.oraxen.api.events.OraxenPlaceEvent;
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
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameplayEventTest extends MechanicTestSupport {

    private final ItemStack hand = mock(ItemStack.class);
    private final Player player = player();
    private final Block block = mock(Block.class);
    private final ItemStack item = mock(ItemStack.class);
    private final Entity baseEntity = mock(Entity.class);
    private final Interaction interaction = mock(Interaction.class);
    private final Drop defaultDrop = Drop.emptyDrop();
    private final Drop replacementDrop = Drop.emptyDrop();

    @Test
    void noteStringAndChorusEventsCarryTheSameFields() {
        NoteBlockMechanic note = blockMechanic(NoteBlockMechanic.class);
        StringBlockMechanic string = blockMechanic(StringBlockMechanic.class);
        ChorusBlockMechanic chorus = blockMechanic(ChorusBlockMechanic.class);

        checkDamage(new OraxenNoteBlockDamageEvent(note, block, player), note);
        checkDamage(new OraxenStringBlockDamageEvent(string, block, player), string);
        checkDamage(new OraxenChorusBlockDamageEvent(chorus, block, player), chorus);

        checkPlace(new OraxenNoteBlockPlaceEvent(note, block, player, item, EquipmentSlot.HAND), note);
        checkPlace(new OraxenStringBlockPlaceEvent(string, block, player, null, EquipmentSlot.OFF_HAND), string);
        assertNull(new OraxenStringBlockPlaceEvent(string, block, player, null, EquipmentSlot.OFF_HAND).getItemInHand());
        checkPlace(new OraxenChorusBlockPlaceEvent(chorus, block, player, item, EquipmentSlot.HAND), chorus);

        checkBreak(new OraxenNoteBlockBreakEvent(note, block, player), note);
        checkBreak(new OraxenStringBlockBreakEvent(string, block, player), string);
        checkBreak(new OraxenChorusBlockBreakEvent(chorus, block, player), chorus);

        OraxenNoteBlockInteractEvent legacy = new OraxenNoteBlockInteractEvent(note, player, item, EquipmentSlot.HAND, block, BlockFace.NORTH);
        checkInteract(legacy, note);
        assertEquals(Action.RIGHT_CLICK_BLOCK, legacy.getAction());
        OraxenNoteBlockInteractEvent clicked = new OraxenNoteBlockInteractEvent(
                note, player, null, EquipmentSlot.OFF_HAND, block, BlockFace.UP, Action.LEFT_CLICK_BLOCK);
        assertNull(clicked.getItemInHand());
        assertEquals(Action.LEFT_CLICK_BLOCK, clicked.getAction());
        assertEquals(BlockFace.UP, clicked.getBlockFace());
        checkInteract(new OraxenStringBlockInteractEvent(string, player, item, EquipmentSlot.HAND, block, BlockFace.EAST), string);
        checkInteract(new OraxenChorusBlockInteractEvent(chorus, player, item, EquipmentSlot.HAND, block, BlockFace.SOUTH), chorus);
    }

    @Test
    void shapedBreakUsesItsOwnHandlerListAndTheMechanicDrop() {
        ShapedBlockMechanic mechanic = blockMechanic(ShapedBlockMechanic.class);
        OraxenShapedBlockBreakEvent event = new OraxenShapedBlockBreakEvent(mechanic, block, player);

        checkBreak(event, mechanic);
        assertSame(OraxenShapedBlockBreakEvent.getHandlerList(), event.getHandlers());
        assertNotSame(OraxenNoteBlockBreakEvent.getHandlerList(), event.getHandlers());
    }

    @Test
    void furnitureAddsTheBaseEntity() {
        FurnitureMechanic mechanic = mock(FurnitureMechanic.class);
        when(mechanic.hasGrowthStages()).thenReturn(false);
        when(mechanic.getDrop()).thenReturn(defaultDrop);
        when(mechanic.getInteractionEntity(baseEntity)).thenReturn(interaction);

        OraxenFurnitureDamageEvent damage = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player);
        checkDamage(damage, mechanic);
        assertNull(damage.getBlock());
        assertSame(baseEntity, damage.getBaseEntity());
        assertSame(block, new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block).getBlock());

        OraxenFurniturePlaceEvent place = new OraxenFurniturePlaceEvent(mechanic, block, baseEntity, player, item, EquipmentSlot.HAND);
        checkPlace(place, mechanic);
        assertSame(baseEntity, place.getBaseEntity());

        OraxenFurnitureBreakEvent broken = new OraxenFurnitureBreakEvent(mechanic, baseEntity, player, null);
        assertSame(mechanic, broken.getMechanic());
        assertSame(player, broken.getPlayer());
        assertNull(broken.getBlock());
        assertSame(baseEntity, broken.getBaseEntity());
        assertSame(defaultDrop, broken.getDrop());
        broken.setDrop(replacementDrop);
        assertSame(replacementDrop, broken.getDrop());
        checkCancellation(broken);

        OraxenFurnitureInteractEvent bare = new OraxenFurnitureInteractEvent(mechanic, baseEntity, player, null, EquipmentSlot.OFF_HAND);
        assertNull(bare.getBlock());
        assertNull(bare.getBlockFace());
        assertNull(bare.getItemInHand());
        assertSame(interaction, bare.getInteractionEntity());
        OraxenFurnitureInteractEvent clicked = new OraxenFurnitureInteractEvent(
                mechanic, baseEntity, player, item, EquipmentSlot.HAND, block, BlockFace.WEST);
        assertSame(block, clicked.getBlock());
        assertSame(BlockFace.WEST, clicked.getBlockFace());
        assertSame(item, clicked.getItemInHand());
        assertSame(baseEntity, clicked.getBaseEntity());
        checkCancellation(clicked);
    }

    @Test
    void eachEventKeepsItsOwnHandlerList() {
        assertNotSame(OraxenNoteBlockPlaceEvent.getHandlerList(), OraxenStringBlockPlaceEvent.getHandlerList());
        assertNotSame(OraxenStringBlockPlaceEvent.getHandlerList(), OraxenChorusBlockPlaceEvent.getHandlerList());
        assertNotSame(OraxenChorusBlockPlaceEvent.getHandlerList(), OraxenFurniturePlaceEvent.getHandlerList());
        assertNotSame(OraxenNoteBlockDamageEvent.getHandlerList(), OraxenNoteBlockBreakEvent.getHandlerList());
        assertNotSame(OraxenNoteBlockBreakEvent.getHandlerList(), OraxenNoteBlockInteractEvent.getHandlerList());
    }

    private <M extends Mechanic> void checkDamage(OraxenDamageEvent<M> event, M mechanic) {
        assertSame(mechanic, event.getMechanic());
        assertSame(player, event.getPlayer());
        if (!(event instanceof OraxenFurnitureDamageEvent damage) || damage.getBlock() != null)
            assertSame(block, event.getBlock());
        assertOwnHandlerList(event);
        checkCancellation(event);
    }

    private <M extends Mechanic> void checkPlace(OraxenPlaceEvent<M> event, M mechanic) {
        checkDamage(event, mechanic);
        assertSame(block, event.getBlock());
        if (event instanceof OraxenStringBlockPlaceEvent) assertNull(event.getItemInHand());
        else assertSame(item, event.getItemInHand());
        assertEquals(event instanceof OraxenStringBlockPlaceEvent ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND, event.getHand());
    }

    private <M extends Mechanic> void checkBreak(OraxenBreakEvent<M> event, M mechanic) {
        checkDamage(event, mechanic);
        assertSame(block, event.getBlock());
        assertSame(defaultDrop, event.getDrop());
        event.setDrop(replacementDrop);
        assertSame(replacementDrop, event.getDrop());
    }

    private <M extends Mechanic> void checkInteract(OraxenInteractEvent<M> event, M mechanic) {
        checkDamage(event, mechanic);
        assertSame(block, event.getBlock());
        assertSame(item, event.getItemInHand());
        assertEquals(EquipmentSlot.HAND, event.getHand());
        assertEquals(event instanceof OraxenStringBlockInteractEvent ? BlockFace.EAST
                : event instanceof OraxenChorusBlockInteractEvent ? BlockFace.SOUTH : BlockFace.NORTH, event.getBlockFace());
    }

    private static void checkCancellation(Cancellable event) {
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    private static void assertOwnHandlerList(Event event) {
        try {
            HandlerList list = (HandlerList) event.getClass().getMethod("getHandlerList").invoke(null);
            assertSame(list, event.getHandlers());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(event.getClass().getName() + " has no getHandlerList()", exception);
        }
    }

    private <M extends Mechanic> M blockMechanic(Class<M> type) {
        M mechanic = mock(type);
        if (mechanic instanceof NoteBlockMechanic note)
            when(note.getDrop(hand)).thenReturn(defaultDrop);
        if (mechanic instanceof StringBlockMechanic string)
            when(string.getDrop(hand)).thenReturn(defaultDrop);
        if (mechanic instanceof ChorusBlockMechanic chorus)
            when(chorus.getDrop(hand)).thenReturn(defaultDrop);
        if (mechanic instanceof ShapedBlockMechanic shaped)
            when(shaped.getDrop(hand)).thenReturn(defaultDrop);
        return mechanic;
    }

    private Player player() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(hand);
        Player player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }
}
