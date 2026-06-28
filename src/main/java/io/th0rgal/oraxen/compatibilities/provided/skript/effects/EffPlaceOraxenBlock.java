package io.th0rgal.oraxen.compatibilities.provided.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import io.th0rgal.oraxen.api.OraxenBlocks;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Effect to place an Oraxen block at a location.
 */
@Name("Place Oraxen Block")
@Description("Places an Oraxen custom block at the specified location.")
@Examples({
        "place oraxen block \"ruby_ore\" at player's location",
        "place oraxen block \"custom_stone\" at {_location}"
})
@Since("1.0")
public class EffPlaceOraxenBlock extends Effect {

    static {
        Skript.registerEffect(EffPlaceOraxenBlock.class,
                "place oraxen block %string% at %locations%",
                "set oraxen block at %locations% to %string%");
    }

    private Expression<String> itemId;
    private Expression<Location> locations;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        if (matchedPattern == 0) {
            itemId = (Expression<String>) exprs[0];
            locations = (Expression<Location>) exprs[1];
        } else {
            locations = (Expression<Location>) exprs[0];
            itemId = (Expression<String>) exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        String id = itemId.getSingle(event);
        if (id == null) return;
        
        Location[] locs = locations.getArray(event);
        if (locs == null) return;
        
        for (Location location : locs) {
            if (location == null) continue;
            OraxenBlocks.place(id, location);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "place oraxen block " + itemId.toString(event, debug) + " at " + locations.toString(event, debug);
    }
}
