package io.th0rgal.oraxen.compatibilities.provided.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Skript event for Oraxen block breaks (NoteBlock and StringBlock).
 */
public class EvtOraxenBlockBreak extends SkriptEvent {

    static {
        Skript.registerEvent("Oraxen Block Break", EvtOraxenBlockBreak.class,
                new Class[]{OraxenNoteBlockBreakEvent.class, OraxenStringBlockBreakEvent.class},
                "oraxen block break [of %-string%]",
                "break[ing] [of] oraxen block [%-string%]")
                .description("Called when a player breaks an Oraxen custom block (NoteBlock or StringBlock).",
                        "You can optionally specify a specific Oraxen item ID to filter for.")
                .examples("on oraxen block break:",
                        "    broadcast \"%player% broke an Oraxen block: %oraxen block id%\"",
                        "on oraxen block break of \"ruby_ore\":",
                        "    give player oraxen item \"ruby\" with amount 3")
                .since("1.0");

        // Register event values for NoteBlock
        EventValues.registerEventValue(OraxenNoteBlockBreakEvent.class, Player.class,
                OraxenNoteBlockBreakEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenNoteBlockBreakEvent.class, Block.class,
                OraxenNoteBlockBreakEvent::getBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenNoteBlockBreakEvent.class, String.class,
                event -> event.getMechanic().getItemID(), EventValues.TIME_NOW);

        // Register event values for StringBlock
        EventValues.registerEventValue(OraxenStringBlockBreakEvent.class, Player.class,
                OraxenStringBlockBreakEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenStringBlockBreakEvent.class, Block.class,
                OraxenStringBlockBreakEvent::getBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenStringBlockBreakEvent.class, String.class,
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
        if (event instanceof OraxenNoteBlockBreakEvent noteEvent) {
            mechanicId = noteEvent.getMechanic().getItemID();
        } else if (event instanceof OraxenStringBlockBreakEvent stringEvent) {
            mechanicId = stringEvent.getMechanic().getItemID();
        }
        
        if (mechanicId == null) return false;
        if (targetId == null) return true;
        
        return mechanicId.equals(targetId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen block break" + (itemId != null ? " of " + itemId.toString(event, debug) : "");
    }
}
