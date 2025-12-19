package io.th0rgal.oraxen.compatibilities.provided.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Skript event for Oraxen furniture placements.
 */
public class EvtOraxenFurniturePlace extends SkriptEvent {

    static {
        Skript.registerEvent("Oraxen Furniture Place", EvtOraxenFurniturePlace.class,
                OraxenFurniturePlaceEvent.class,
                "oraxen furniture place [of %-string%]",
                "plac(e|ing) [of] oraxen furniture [%-string%]")
                .description("Called when a player places Oraxen furniture.",
                        "You can optionally specify a specific Oraxen item ID to filter for.")
                .examples("on oraxen furniture place:",
                        "    broadcast \"%player% placed furniture: %oraxen furniture id%\"",
                        "on oraxen furniture place of \"lamp\":",
                        "    broadcast \"A lamp was placed!\"")
                .since("1.0");

        // Register event values
        EventValues.registerEventValue(OraxenFurniturePlaceEvent.class, Player.class, 
                OraxenFurniturePlaceEvent::getPlayer);
        EventValues.registerEventValue(OraxenFurniturePlaceEvent.class, Block.class, 
                OraxenFurniturePlaceEvent::getBlock);
        EventValues.registerEventValue(OraxenFurniturePlaceEvent.class, Entity.class, 
                OraxenFurniturePlaceEvent::getBaseEntity);
        EventValues.registerEventValue(OraxenFurniturePlaceEvent.class, ItemStack.class, 
                OraxenFurniturePlaceEvent::getItemInHand);
        EventValues.registerEventValue(OraxenFurniturePlaceEvent.class, String.class, 
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
        if (!(event instanceof OraxenFurniturePlaceEvent furnitureEvent)) return false;
        
        String targetId = itemId != null ? itemId.getSingle() : null;
        String mechanicId = furnitureEvent.getMechanic().getItemID();
        
        if (targetId == null) return true;
        return mechanicId.equals(targetId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen furniture place" + (itemId != null ? " of " + itemId.toString(event, debug) : "");
    }
}
