package io.th0rgal.oraxen.compatibilities.provided.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Condition to check if a block is an Oraxen block.
 */
@Name("Is Oraxen Block")
@Description("Checks if a block or location is an Oraxen custom block, optionally checking for a specific ID.")
@Examples({
        "if clicked block is an oraxen block:",
        "    cancel event",
        "if block at player's location is oraxen block \"ruby_ore\":",
        "    broadcast \"You found ruby ore!\""
})
@Since("1.0")
public class CondIsOraxenBlock extends Condition {

    static {
        Skript.registerCondition(CondIsOraxenBlock.class,
                "%blocks/locations% (is|are) [a[n]] oraxen block[s]",
                "%blocks/locations% (is|are) oraxen block[s] %string%",
                "%blocks/locations% (isn't|is not|aren't|are not) [a[n]] oraxen block[s]",
                "%blocks/locations% (isn't|is not|aren't|are not) oraxen block[s] %string%");
    }

    private Expression<?> blocks;
    private Expression<String> specificId;
    private boolean negated;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        blocks = exprs[0];
        specificId = exprs.length > 1 ? (Expression<String>) exprs[1] : null;
        negated = matchedPattern >= 2;
        return true;
    }

    @Override
    public boolean check(Event event) {
        String targetId = specificId != null ? specificId.getSingle(event) : null;
        
        boolean result = blocks.check(event, obj -> {
            Location location = null;
            if (obj instanceof Block block) {
                location = block.getLocation();
            } else if (obj instanceof Location loc) {
                location = loc;
            }
            
            if (location == null) return false;
            
            Mechanic mechanic = OraxenBlocks.getOraxenBlock(location);
            if (mechanic == null) return false;
            
            if (targetId != null) {
                return mechanic.getItemID().equals(targetId);
            }
            return true;
        });
        
        return negated != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String base = blocks.toString(event, debug) + (negated ? " is not" : " is") + " an oraxen block";
        if (specificId != null) {
            base += " " + specificId.toString(event, debug);
        }
        return base;
    }
}
