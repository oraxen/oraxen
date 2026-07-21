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
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Condition to check if an ItemStack is an Oraxen item.
 */
@Name("Is Oraxen Item")
@Description("Checks if an item is an Oraxen item, optionally checking for a specific ID.")
@Examples({
        "if player's tool is an oraxen item:",
        "    broadcast \"You're holding an Oraxen item!\"",
        "if player's tool is oraxen item \"ruby_sword\":",
        "    broadcast \"You have the Ruby Sword!\""
})
@Since("1.0")
public class CondIsOraxenItem extends Condition {

    static {
        Skript.registerCondition(CondIsOraxenItem.class,
                "%itemstacks% (is|are) [a[n]] oraxen item[s]",
                "%itemstacks% (is|are) oraxen item[s] %string%",
                "%itemstacks% (isn't|is not|aren't|are not) [a[n]] oraxen item[s]",
                "%itemstacks% (isn't|is not|aren't|are not) oraxen item[s] %string%");
    }

    private Expression<ItemStack> items;
    private Expression<String> specificId;
    private boolean negated;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        items = (Expression<ItemStack>) exprs[0];
        specificId = exprs.length > 1 ? (Expression<String>) exprs[1] : null;
        negated = matchedPattern >= 2;
        return true;
    }

    @Override
    public boolean check(Event event) {
        String targetId = specificId != null ? specificId.getSingle(event) : null;
        
        boolean result = items.check(event, item -> {
            if (item == null) return false;
            String itemId = OraxenItems.getIdByItem(item);
            if (itemId == null) return false;
            
            if (targetId != null) {
                return itemId.equals(targetId);
            }
            return true;
        });
        
        return negated != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String base = items.toString(event, debug) + (negated ? " is not" : " is") + " an oraxen item";
        if (specificId != null) {
            base += " " + specificId.toString(event, debug);
        }
        return base;
    }
}
