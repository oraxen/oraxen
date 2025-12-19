package io.th0rgal.oraxen.compatibilities.provided.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * Registers custom Skript types for Oraxen
 */
public class OraxenSkriptTypes {

    /**
     * Wrapper class representing an Oraxen item by its ID
     */
    public static class OraxenItemId {
        private final String id;

        public OraxenItemId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public boolean exists() {
            return OraxenItems.exists(id);
        }

        public boolean isBlock() {
            return OraxenBlocks.isOraxenBlock(id);
        }

        public boolean isFurniture() {
            return OraxenFurniture.isFurniture(id);
        }

        @Nullable
        public ItemBuilder getItemBuilder() {
            return OraxenItems.getItemById(id);
        }

        @Override
        public String toString() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            OraxenItemId that = (OraxenItemId) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Register all Oraxen types with Skript
     */
    public static void register() {
        // Register OraxenItemId type
        Classes.registerClass(new ClassInfo<>(OraxenItemId.class, "oraxenitemid")
                .user("oraxen ?item ?ids?")
                .name("Oraxen Item ID")
                .description("Represents an Oraxen item ID that can be used to get items, check blocks, or furniture.")
                .usage("\"item_id\"")
                .examples("set {_id} to oraxen id of player's tool", "if oraxen id of clicked block is \"ruby_ore\":")
                .since("1.0")
                .parser(new Parser<OraxenItemId>() {
                    @Override
                    @Nullable
                    public OraxenItemId parse(String input, ParseContext context) {
                        // Remove quotes if present
                        String id = input.trim();
                        if (id.startsWith("\"") && id.endsWith("\"")) {
                            id = id.substring(1, id.length() - 1);
                        }
                        if (OraxenItems.exists(id)) {
                            return new OraxenItemId(id);
                        }
                        return null;
                    }

                    @Override
                    public String toString(OraxenItemId itemId, int flags) {
                        return itemId.getId();
                    }

                    @Override
                    public String toVariableNameString(OraxenItemId itemId) {
                        return "oraxen:" + itemId.getId();
                    }
                }));
    }
}
