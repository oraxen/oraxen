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
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Condition to check if an entity, block, or location is Oraxen furniture.
 */
@Name("Is Oraxen Furniture")
@Description("Checks if an entity, block, or location is Oraxen furniture, optionally checking for a specific ID.")
@Examples({
        "if target entity is oraxen furniture:",
        "    broadcast \"You're looking at furniture!\"",
        "if clicked entity is oraxen furniture \"wooden_chair\":",
        "    make player sit on clicked entity"
})
@Since("1.0")
public class CondIsOraxenFurniture extends Condition {

    static {
        Skript.registerCondition(CondIsOraxenFurniture.class,
                "%entities/blocks/locations% (is|are) [a[n]] oraxen furniture",
                "%entities/blocks/locations% (is|are) oraxen furniture %string%",
                "%entities/blocks/locations% (isn't|is not|aren't|are not) [a[n]] oraxen furniture",
                "%entities/blocks/locations% (isn't|is not|aren't|are not) oraxen furniture %string%");
    }

    private Expression<?> targets;
    private Expression<String> specificId;
    private boolean negated;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        targets = exprs[0];
        specificId = exprs.length > 1 ? (Expression<String>) exprs[1] : null;
        negated = matchedPattern >= 2;
        return true;
    }

    @Override
    public boolean check(Event event) {
        String targetId = specificId != null ? specificId.getSingle(event) : null;
        
        boolean result = targets.check(event, obj -> {
            FurnitureMechanic mechanic = null;
            
            if (obj instanceof Entity entity) {
                mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            } else if (obj instanceof Block block) {
                mechanic = OraxenFurniture.getFurnitureMechanic(block);
            } else if (obj instanceof Location location) {
                mechanic = OraxenFurniture.getFurnitureMechanic(location.getBlock());
            }
            
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
        String base = targets.toString(event, debug) + (negated ? " is not" : " is") + " oraxen furniture";
        if (specificId != null) {
            base += " " + specificId.toString(event, debug);
        }
        return base;
    }
}
