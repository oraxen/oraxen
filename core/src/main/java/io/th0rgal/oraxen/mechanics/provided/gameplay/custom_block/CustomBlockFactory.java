package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CustomBlockFactory extends MechanicFactory {

    private static CustomBlockFactory instance;

    public CustomBlockFactory(String mechanicId) {
        super(mechanicId);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new CustomBlockListener(), new CustomBlockMiningListener());
    }

    public static boolean isEnabled() {
        return instance != null;
    }

    public static CustomBlockFactory getInstance() {
        return instance;
    }

    public List<String> toolTypes(CustomBlockType type) {
        if (type == CustomBlockType.STRINGBLOCK && StringBlockMechanicFactory.isEnabled())
            return StringBlockMechanicFactory.getInstance().toolTypes;
        else if (type == CustomBlockType.NOTEBLOCK && NoteBlockMechanicFactory.isEnabled())
            return NoteBlockMechanicFactory.getInstance().toolTypes;
        else return new ArrayList<>();
    }

    @Override
    public CustomBlockMechanic parse(ConfigurationSection section) {
        String itemId = section.getParent().getParent().getName();
        CustomBlockType type = CustomBlockType.fromString(section.getString("type", ""));
        CustomBlockMechanic mechanic = null;
        if (type == null) {
            Logs.logError("No CustomBlock-type defined in " + itemId);
            Logs.logError("Valid types are: " + StringUtils.join(CustomBlockType.names()));
        } else if (type == CustomBlockType.NOTEBLOCK) {
            if (NoteBlockMechanicFactory.isEnabled())
                mechanic = NoteBlockMechanicFactory.getInstance().parse(section);
            else Logs.logError(itemId + " attempted to use " + type.name() + "-type but it has been disabled");
        } else if (type == CustomBlockType.STRINGBLOCK) {
            if (StringBlockMechanicFactory.isEnabled())
                mechanic = StringBlockMechanicFactory.getInstance().parse(section);
            else Logs.logError(itemId + " attempted to use " + type.name() + "-type but it has been disabled");
        }

        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public CustomBlockMechanic getMechanic(String itemID) {
        return (CustomBlockMechanic) super.getMechanic(itemID);
    }

    @Override
    public CustomBlockMechanic getMechanic(ItemStack itemStack) {
        return (CustomBlockMechanic) super.getMechanic(itemStack);
    }
}
