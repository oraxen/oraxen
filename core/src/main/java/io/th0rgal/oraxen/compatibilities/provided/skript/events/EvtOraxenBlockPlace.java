package io.th0rgal.oraxen.compatibilities.provided.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Skript event for Oraxen block placements (NoteBlock and StringBlock).
 */
public class EvtOraxenBlockPlace extends SkriptEvent {

    static {
        Skript.registerEvent("Oraxen Block Place", EvtOraxenBlockPlace.class,
                new Class[]{OraxenNoteBlockPlaceEvent.class, OraxenStringBlockPlaceEvent.class},
                "oraxen block place [of %-string%]",
                "plac(e|ing) [of] oraxen block [%-string%]")
                .description("Called when a player places an Oraxen custom block (NoteBlock or StringBlock).",
                        "You can optionally specify a specific Oraxen item ID to filter for.")
                .examples("on oraxen block place:",
                        "    broadcast \"%player% placed an Oraxen block: %oraxen block id%\"",
                        "on oraxen block place of \"ruby_ore\":",
                        "    broadcast \"Ruby ore placed!\"")
                .since("1.0");

        // Register event values for NoteBlock
        EventValues.registerEventValue(OraxenNoteBlockPlaceEvent.class, Player.class, 
                OraxenNoteBlockPlaceEvent::getPlayer);
        EventValues.registerEventValue(OraxenNoteBlockPlaceEvent.class, Block.class, 
                OraxenNoteBlockPlaceEvent::getBlock);
        EventValues.registerEventValue(OraxenNoteBlockPlaceEvent.class, ItemStack.class, 
                OraxenNoteBlockPlaceEvent::getItemInHand);
        EventValues.registerEventValue(OraxenNoteBlockPlaceEvent.class, String.class, 
                event -> event.getMechanic().getItemID());

        // Register event values for StringBlock
        EventValues.registerEventValue(OraxenStringBlockPlaceEvent.class, Player.class, 
                OraxenStringBlockPlaceEvent::getPlayer);
        EventValues.registerEventValue(OraxenStringBlockPlaceEvent.class, Block.class, 
                OraxenStringBlockPlaceEvent::getBlock);
        EventValues.registerEventValue(OraxenStringBlockPlaceEvent.class, ItemStack.class, 
                OraxenStringBlockPlaceEvent::getItemInHand);
        EventValues.registerEventValue(OraxenStringBlockPlaceEvent.class, String.class, 
                event -> event.getMechanic().getItemID());
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
        if (event instanceof OraxenNoteBlockPlaceEvent noteEvent) {
            mechanicId = noteEvent.getMechanic().getItemID();
        } else if (event instanceof OraxenStringBlockPlaceEvent stringEvent) {
            mechanicId = stringEvent.getMechanic().getItemID();
        }
        
        if (mechanicId == null) return false;
        if (targetId == null) return true;
        
        return mechanicId.equals(targetId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen block place" + (itemId != null ? " of " + itemId.toString(event, debug) : "");
    }
}
