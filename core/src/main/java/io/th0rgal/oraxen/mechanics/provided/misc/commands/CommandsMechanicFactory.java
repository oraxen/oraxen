package io.th0rgal.oraxen.mechanics.provided.misc.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.NestedProperty;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(category = "misc", description = "Executes commands when the item is used")
public class CommandsMechanicFactory extends MechanicFactory {

    @ConfigProperty(type = PropertyType.STRING, description = "Permission required to use the item")
    public static final String PROP_PERMISSION = "permission";

    @ConfigProperty(type = PropertyType.INTEGER, description = "Cooldown between uses in ticks", defaultValue = "0", min = 0)
    public static final String PROP_COOLDOWN = "cooldown";

    @ConfigProperty(type = PropertyType.BOOLEAN, description = "Whether the item is consumed on use", defaultValue = "false")
    public static final String PROP_ONE_USAGE = "one_usage";

    @ConfigProperty(type = PropertyType.OBJECT, description = "Commands executed as console", nested = @NestedProperty(name = "commands", type = PropertyType.LIST, description = "List of commands to execute"))
    public static final String PROP_CONSOLE = "console";

    @ConfigProperty(type = PropertyType.OBJECT, description = "Commands executed as player", nested = @NestedProperty(name = "commands", type = PropertyType.LIST, description = "List of commands to execute"))
    public static final String PROP_PLAYER = "player";

    @ConfigProperty(type = PropertyType.OBJECT, description = "Commands executed as opped player", nested = @NestedProperty(name = "commands", type = PropertyType.LIST, description = "List of commands to execute"))
    public static final String PROP_OPPED_PLAYER = "opped_player";

    public CommandsMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new CommandsMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new CommandsMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}
