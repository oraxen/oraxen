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
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Effect to remove Oraxen furniture at a location or entity.
 */
@Name("Remove Oraxen Furniture")
@Description("Removes Oraxen furniture at the specified location or entity without drops.")
@Examples({
        "remove oraxen furniture at player's location",
        "remove oraxen furniture at target entity"
})
@Since("1.0")
public class EffRemoveOraxenFurniture extends Effect {

    static {
        Skript.registerEffect(EffRemoveOraxenFurniture.class,
                "remove oraxen furniture at %locations/entities%",
                "delete oraxen furniture at %locations/entities%");
    }

    private Expression<?> targets;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        targets = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object[] objs = targets.getArray(event);
        if (objs == null) return;
        
        for (Object obj : objs) {
            if (obj instanceof Entity entity) {
                OraxenFurniture.remove(entity, null);
            } else if (obj instanceof Location location) {
                OraxenFurniture.remove(location, null);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "remove oraxen furniture at " + targets.toString(event, debug);
    }
}
