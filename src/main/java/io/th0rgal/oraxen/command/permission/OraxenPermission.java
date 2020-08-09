package io.th0rgal.oraxen.command.permission;

import org.bukkit.command.CommandSender;

public enum OraxenPermission {

    //
    // General Permissions
    //
    ALL,

    //
    // Command Permissions
    //
    COMMAND_ALL(ALL),

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
    COMMAND_REPAIR_EVERYTHING(COMMAND_REPAIR_ALL)

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
     * Permission check
     */

    public boolean has(CommandSender sender) {
        return sender.hasPermission(asString()) ? true : ((parent == null) ? false : parent.has(sender));
    }

    /*
     * String functions
     */

    public String asString() {
        return toString();
    }

    @Override
    public String toString() {
        String name = name().toLowerCase();
        if (name.endsWith("_ALL"))
            name = name.substring(0, name.length() - 4) + '*';
        else if (name.equals("ALL"))
            name = "*";
        return String.format(PERMISSION_FORMAT, prefix, name);
    }

}
