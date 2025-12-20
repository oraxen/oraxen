package io.th0rgal.oraxen.compatibilities.provided.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Expression to get the Oraxen furniture ID at a location or from an entity.
 */
@Name("Oraxen Furniture")
@Description("Gets the Oraxen furniture ID at a location, from a block, or from an entity. Returns the ID string if it's Oraxen furniture, otherwise nothing.")
@Examples({
        "set {_furnitureId} to oraxen furniture at player's location",
        "if oraxen furniture of target entity exists:",
        "    broadcast \"This is Oraxen furniture: %oraxen furniture of target entity%\""
})
@Since("1.0")
public class ExprOraxenFurniture extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprOraxenFurniture.class, String.class, ExpressionType.COMBINED,
                "[the] oraxen furniture [id] (at|of) %locations/blocks/entities%",
                "%locations/blocks/entities%'[s] oraxen furniture [id]");
    }

    private Expression<?> targets;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        targets = exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected String[] get(Event event) {
        Object[] objs = targets.getArray(event);
        if (objs == null || objs.length == 0) return null;

        return java.util.Arrays.stream(objs)
                .map(this::getFurnitureId)
                .filter(java.util.Objects::nonNull)
                .toArray(String[]::new);
    }

    @Nullable
    private String getFurnitureId(Object obj) {
        FurnitureMechanic mechanic = null;
        
        if (obj instanceof Entity entity) {
            mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        } else if (obj instanceof Block block) {
            mechanic = OraxenFurniture.getFurnitureMechanic(block);
        } else if (obj instanceof Location location) {
            mechanic = OraxenFurniture.getFurnitureMechanic(location.getBlock());
        }
        
        return mechanic != null ? mechanic.getItemID() : null;
    }

    @Override
    public boolean isSingle() {
        return targets.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen furniture at " + targets.toString(event, debug);
    }
}
