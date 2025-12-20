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
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Expression to get the Oraxen ID from various objects.
 */
@Name("Oraxen ID")
@Description("Gets the Oraxen ID from an ItemStack, Block, Entity, or Location.")
@Examples({
        "set {_id} to oraxen id of player's tool",
        "if oraxen id of clicked block is \"ruby_ore\":",
        "broadcast oraxen id of target entity"
})
@Since("1.0")
public class ExprOraxenId extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprOraxenId.class, String.class, ExpressionType.PROPERTY,
                "[the] oraxen id of %itemstacks/blocks/entities/locations%",
                "%itemstacks/blocks/entities/locations%'[s] oraxen id");
    }

    private Expression<?> objects;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        objects = exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected String[] get(Event event) {
        Object[] objs = objects.getArray(event);
        if (objs == null || objs.length == 0) return null;

        return java.util.Arrays.stream(objs)
                .map(this::getOraxenId)
                .filter(java.util.Objects::nonNull)
                .toArray(String[]::new);
    }

    @Nullable
    private String getOraxenId(Object obj) {
        if (obj instanceof ItemStack item) {
            return OraxenItems.getIdByItem(item);
        } else if (obj instanceof Block block) {
            Mechanic mechanic = OraxenBlocks.getOraxenBlock(block.getLocation());
            return mechanic != null ? mechanic.getItemID() : null;
        } else if (obj instanceof Entity entity) {
            FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
            return mechanic != null ? mechanic.getItemID() : null;
        } else if (obj instanceof Location location) {
            // Try block first
            Mechanic blockMechanic = OraxenBlocks.getOraxenBlock(location);
            if (blockMechanic != null) return blockMechanic.getItemID();
            
            // Try furniture
            FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(location.getBlock());
            return furnitureMechanic != null ? furnitureMechanic.getItemID() : null;
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return objects.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen id of " + objects.toString(event, debug);
    }
}
