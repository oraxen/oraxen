package io.th0rgal.oraxen.compatibilities.provided.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Skript event for Oraxen block interactions (NoteBlock and StringBlock).
 */
public class EvtOraxenBlockInteract extends SkriptEvent {

    static {
        Skript.registerEvent("Oraxen Block Interact", EvtOraxenBlockInteract.class,
                new Class[]{OraxenNoteBlockInteractEvent.class, OraxenStringBlockInteractEvent.class},
                "oraxen block interact [of %-string%]",
                "interact[ing] with oraxen block [%-string%]")
                .description("Called when a player interacts with an Oraxen custom block (NoteBlock or StringBlock).",
                        "You can optionally specify a specific Oraxen item ID to filter for.")
                .examples("on oraxen block interact:",
                        "    broadcast \"%player% interacted with an Oraxen block!\"",
                        "on oraxen block interact of \"magic_chest\":",
                        "    open virtual chest inventory to player")
                .since("1.0");

        // Register event values for NoteBlock
        EventValues.registerEventValue(OraxenNoteBlockInteractEvent.class, Player.class,
                OraxenNoteBlockInteractEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenNoteBlockInteractEvent.class, Block.class,
                OraxenNoteBlockInteractEvent::getBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenNoteBlockInteractEvent.class, ItemStack.class,
                OraxenNoteBlockInteractEvent::getItemInHand, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenNoteBlockInteractEvent.class, String.class,
                event -> event.getMechanic().getItemID(), EventValues.TIME_NOW);

        // Register event values for StringBlock
        EventValues.registerEventValue(OraxenStringBlockInteractEvent.class, Player.class,
                OraxenStringBlockInteractEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenStringBlockInteractEvent.class, Block.class,
                OraxenStringBlockInteractEvent::getBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenStringBlockInteractEvent.class, ItemStack.class,
                OraxenStringBlockInteractEvent::getItemInHand, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenStringBlockInteractEvent.class, String.class,
                event -> event.getMechanic().getItemID(), EventValues.TIME_NOW);
    }

    private Literal<String> itemId;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        itemId = (Literal<String>) args[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        String targetId = itemId != null ? itemId.getSingle() : null;
        
        String mechanicId = null;
        if (event instanceof OraxenNoteBlockInteractEvent noteEvent) {
            mechanicId = noteEvent.getMechanic().getItemID();
        } else if (event instanceof OraxenStringBlockInteractEvent stringEvent) {
            mechanicId = stringEvent.getMechanic().getItemID();
        }
        
        if (mechanicId == null) return false;
        if (targetId == null) return true;
        
        return mechanicId.equals(targetId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen block interact" + (itemId != null ? " of " + itemId.toString(event, debug) : "");
    }
}
