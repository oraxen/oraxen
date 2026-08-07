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
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Effect to remove an Oraxen block at a location.
 */
@Name("Remove Oraxen Block")
@Description("Removes an Oraxen custom block at the specified location without drops.")
@Examples({
        "remove oraxen block at player's location",
        "remove oraxen block at clicked block"
})
@Since("1.0")
public class EffRemoveOraxenBlock extends Effect {

    static {
        Skript.registerEffect(EffRemoveOraxenBlock.class,
                "remove oraxen block at %locations/blocks%",
                "delete oraxen block at %locations/blocks%");
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
            Location location = null;
            if (obj instanceof Location loc) {
                location = loc;
            } else if (obj instanceof Block block) {
                location = block.getLocation();
            }
            
            if (location == null) continue;
            OraxenBlocks.remove(location, null);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "remove oraxen block at " + targets.toString(event, debug);
    }
}
