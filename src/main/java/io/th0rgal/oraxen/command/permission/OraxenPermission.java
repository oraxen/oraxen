package io.th0rgal.oraxen.command.permission;

import org.bukkit.command.CommandSender;

import io.th0rgal.oraxen.language.Message;
import io.th0rgal.oraxen.language.Variable;
import io.th0rgal.oraxen.utils.general.Placeholder;

public enum OraxenPermission implements IPermission {

    //
    // General Permissions
    //
    ALL,

    //
    // Command Permissions
    //
    COMMAND_ALL(ALL),
    
    // Give
    COMMAND_GIVE(COMMAND_ALL),
    
    // Pack
    COMMAND_PACK(COMMAND_ALL),

    // Reload
    COMMAND_RELOAD(COMMAND_ALL),

    // Help
    COMMAND_HELP(COMMAND_ALL),

    // Debug
    COMMAND_DEBUG(COMMAND_ALL),

    // Inventory
    COMMAND_INVENTORY(COMMAND_ALL),

    // Repair
    COMMAND_REPAIR_ALL(COMMAND_ALL),
    COMMAND_REPAIR(COMMAND_REPAIR_ALL),
    COMMAND_REPAIR_EVERYTHING(COMMAND_REPAIR_ALL),
    
    // Recipe
    COMMAND_RECIPE_EDIT(COMMAND_ALL),
    COMMAND_RECIPE(COMMAND_RECIPE_EDIT),

    //
    // END
    //
    ;

    public static final String PERMISSION_FORMAT = "%s.%s";

    private final String prefix;
    private final OraxenPermission parent;

    private OraxenPermission() {
        this.prefix = "oraxen";
        this.parent = null;
    }

    private OraxenPermission(OraxenPermission parent) {
        this.prefix = "oraxen";
        this.parent = parent;
    }

    private OraxenPermission(String prefix) {
        this.prefix = prefix;
        this.parent = null;
    }

    private OraxenPermission(String prefix, OraxenPermission parent) {
        this.prefix = prefix;
        this.parent = parent;
    }

    /*
     * IPermission implementation
     */

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public OraxenPermission parent() {
        return parent;
    }

    /*
     * Permission check
     */

    public boolean required(CommandSender sender) {
        if (!has(sender)) {
            Message.NO_PERMISSION
                    .send(sender, Variable.PREFIX
                            .placeholder(),
                        getPlaceholder());
            return false;
        }
        return true;
    }

    public boolean required(CommandSender sender, Placeholder... placeholders) {
        if (!has(sender)) {
            Message.NO_PERMISSION
                    .send(sender, placeholders, Variable.PREFIX
                            .placeholder(),
                        getPlaceholder());
            return false;
        }
        return true;
    }

    /*
     * String functions
     */

    @Override
    public String toString() {
        return asString();
    }

}
