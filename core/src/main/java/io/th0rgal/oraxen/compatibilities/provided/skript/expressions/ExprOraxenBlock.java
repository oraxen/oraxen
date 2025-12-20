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
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Expression to get the Oraxen block ID at a location or block.
 */
@Name("Oraxen Block")
@Description("Gets the Oraxen block ID at a location or from a block. Returns the ID string if it's an Oraxen block, otherwise nothing.")
@Examples({
        "set {_blockId} to oraxen block at player's location",
        "if oraxen block at clicked block exists:",
        "    broadcast \"This is an Oraxen block: %oraxen block at clicked block%\""
})
@Since("1.0")
public class ExprOraxenBlock extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprOraxenBlock.class, String.class, ExpressionType.COMBINED,
                "[the] oraxen block [id] (at|of) %locations/blocks%",
                "%locations/blocks%'[s] oraxen block [id]");
    }

    private Expression<?> locations;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        locations = exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected String[] get(Event event) {
        Object[] locs = locations.getArray(event);
        if (locs == null || locs.length == 0) return null;

        return java.util.Arrays.stream(locs)
                .map(this::getBlockId)
                .filter(java.util.Objects::nonNull)
                .toArray(String[]::new);
    }

    @Nullable
    private String getBlockId(Object obj) {
        Location location = null;
        if (obj instanceof Location loc) {
            location = loc;
        } else if (obj instanceof Block block) {
            location = block.getLocation();
        }
        
        if (location == null) return null;
        
        Mechanic mechanic = OraxenBlocks.getOraxenBlock(location);
        return mechanic != null ? mechanic.getItemID() : null;
    }

    @Override
    public boolean isSingle() {
        return locations.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen block at " + locations.toString(event, debug);
    }
}
