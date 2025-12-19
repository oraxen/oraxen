package io.th0rgal.oraxen.compatibilities.provided.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Skript event for Oraxen furniture interactions.
 */
public class EvtOraxenFurnitureInteract extends SkriptEvent {

    static {
        Skript.registerEvent("Oraxen Furniture Interact", EvtOraxenFurnitureInteract.class,
                OraxenFurnitureInteractEvent.class,
                "oraxen furniture interact [of %-string%]",
                "interact[ing] with oraxen furniture [%-string%]")
                .description("Called when a player interacts with Oraxen furniture.",
                        "You can optionally specify a specific Oraxen item ID to filter for.")
                .examples("on oraxen furniture interact:",
                        "    broadcast \"%player% interacted with furniture!\"",
                        "on oraxen furniture interact of \"treasure_chest\":",
                        "    give player diamond")
                .since("1.0");

        // Register event values
        EventValues.registerEventValue(OraxenFurnitureInteractEvent.class, Player.class, 
                OraxenFurnitureInteractEvent::getPlayer);
        EventValues.registerEventValue(OraxenFurnitureInteractEvent.class, Block.class, 
                OraxenFurnitureInteractEvent::getBlock);
        EventValues.registerEventValue(OraxenFurnitureInteractEvent.class, Entity.class, 
                OraxenFurnitureInteractEvent::getBaseEntity);
        EventValues.registerEventValue(OraxenFurnitureInteractEvent.class, ItemStack.class, 
                OraxenFurnitureInteractEvent::getItemInHand);
        EventValues.registerEventValue(OraxenFurnitureInteractEvent.class, String.class, 
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
        if (!(event instanceof OraxenFurnitureInteractEvent furnitureEvent)) return false;
        
        String targetId = itemId != null ? itemId.getSingle() : null;
        String mechanicId = furnitureEvent.getMechanic().getItemID();
        
        if (targetId == null) return true;
        return mechanicId.equals(targetId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen furniture interact" + (itemId != null ? " of " + itemId.toString(event, debug) : "");
    }
}
