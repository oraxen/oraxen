package org.playuniverse.snowypine.command.permission;

import org.bukkit.command.CommandSender;
import org.playuniverse.snowypine.language.IPlaceable;
import org.playuniverse.snowypine.utils.general.Placeholder;

public interface IPermission extends IPlaceable {

    public static final String PERMISSION_FORMAT = "%s.%s";

    public String name();

    public String prefix();

    public IPermission parent();

    @Override
    public default Placeholder getPlaceholder() {
        return new Placeholder("permission", asString());
    }

    public default boolean has(CommandSender sender) {
        IPermission parent = parent();
        return sender.hasPermission(asString()) || (parent != null && parent.has(sender));
    }

    public boolean required(CommandSender sender);

    public boolean required(CommandSender sender, Placeholder... placeholders);

    public default String asString() {
        String name = name().toLowerCase();
        if (name.endsWith("_ALL"))
            name = name.substring(0, name.length() - 4) + '*';
        else if (name.equals("ALL"))
            name = "*";
        return String.format(PERMISSION_FORMAT, prefix(), name.replace('_', '.'));
    }

}
