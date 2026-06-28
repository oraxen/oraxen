package io.th0rgal.oraxen.compatibilities.provided.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Skript event for Oraxen furniture breaks.
 */
public class EvtOraxenFurnitureBreak extends SkriptEvent {

    static {
        Skript.registerEvent("Oraxen Furniture Break", EvtOraxenFurnitureBreak.class,
                OraxenFurnitureBreakEvent.class,
                "oraxen furniture break [of %-string%]",
                "break[ing] [of] oraxen furniture [%-string%]")
                .description("Called when a player breaks Oraxen furniture.",
                        "You can optionally specify a specific Oraxen item ID to filter for.")
                .examples("on oraxen furniture break:",
                        "    broadcast \"%player% broke furniture: %oraxen furniture id%\"",
                        "on oraxen furniture break of \"wooden_chair\":",
                        "    cancel event",
                        "    send \"You can't break this chair!\" to player")
                .since("1.0");

        // Register event values
        EventValues.registerEventValue(OraxenFurnitureBreakEvent.class, Player.class,
                OraxenFurnitureBreakEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenFurnitureBreakEvent.class, Block.class,
                OraxenFurnitureBreakEvent::getBlock, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenFurnitureBreakEvent.class, Entity.class,
                OraxenFurnitureBreakEvent::getBaseEntity, EventValues.TIME_NOW);
        EventValues.registerEventValue(OraxenFurnitureBreakEvent.class, String.class,
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
        if (!(event instanceof OraxenFurnitureBreakEvent furnitureEvent)) return false;
        
        String targetId = itemId != null ? itemId.getSingle() : null;
        String mechanicId = furnitureEvent.getMechanic().getItemID();
        
        if (targetId == null) return true;
        return mechanicId.equals(targetId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen furniture break" + (itemId != null ? " of " + itemId.toString(event, debug) : "");
    }
}
