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
import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Effect to place Oraxen furniture at a location.
 */
@Name("Place Oraxen Furniture")
@Description("Places Oraxen furniture at the specified location with optional rotation.")
@Examples({
        "place oraxen furniture \"wooden_chair\" at player's location",
        "place oraxen furniture \"lamp\" at {_location}"
})
@Since("1.0")
public class EffPlaceOraxenFurniture extends Effect {

    static {
        Skript.registerEffect(EffPlaceOraxenFurniture.class,
                "place oraxen furniture %string% at %locations%",
                "spawn oraxen furniture %string% at %locations%");
    }

    private Expression<String> itemId;
    private Expression<Location> locations;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        itemId = (Expression<String>) exprs[0];
        locations = (Expression<Location>) exprs[1];
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
            OraxenFurniture.place(id, location, Rotation.NONE, BlockFace.UP);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "place oraxen furniture " + itemId.toString(event, debug) + " at " + locations.toString(event, debug);
    }
}
