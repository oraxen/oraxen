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
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Expression to get an Oraxen ItemStack from its ID.
 */
@Name("Oraxen Item")
@Description("Gets an Oraxen item as an ItemStack from its ID.")
@Examples({
        "give player oraxen item \"ruby_sword\"",
        "set {_item} to oraxen item \"emerald_pickaxe\"",
        "drop oraxen item \"custom_helmet\" at player's location"
})
@Since("1.0")
public class ExprOraxenItem extends SimpleExpression<ItemStack> {

    static {
        Skript.registerExpression(ExprOraxenItem.class, ItemStack.class, ExpressionType.COMBINED,
                "[the] oraxen item[s] %strings%",
                "[the] oraxen item[s] (from|with) id[s] %strings%");
    }

    private Expression<String> itemIds;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        itemIds = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected ItemStack[] get(Event event) {
        String[] ids = itemIds.getArray(event);
        if (ids == null || ids.length == 0) return null;

        return java.util.Arrays.stream(ids)
                .map(OraxenItems::getItemById)
                .filter(java.util.Objects::nonNull)
                .map(ItemBuilder::build)
                .toArray(ItemStack[]::new);
    }

    @Override
    public boolean isSingle() {
        return itemIds.isSingle();
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "oraxen item " + itemIds.toString(event, debug);
    }
}
