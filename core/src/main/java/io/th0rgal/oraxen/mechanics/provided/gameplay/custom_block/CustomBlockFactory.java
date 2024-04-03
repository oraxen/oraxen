package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class CustomBlockFactory extends MechanicFactory {

    public CustomBlockFactory(ConfigurationSection section) {
        super(section);

        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new CustomBlockListener());
    }

    @Override
    public CustomBlockMechanic parse(ConfigurationSection section) {
        CustomBlockMechanic mechanic;
        CustomBlockType type = CustomBlockType.fromString(section.getString("type", ""));
        if (type == null) {
            Logs.logError("No CustomBlockType defined in " + section.getParent().getParent().getName());
            Logs.logError("Valid types are: " + StringUtils.join(CustomBlockType.names()));
            mechanic = null;
        } else if (type == CustomBlockType.NOTEBLOCK) {
            mechanic = new NoteBlockMechanic(this, section);
        } else if (type == CustomBlockType.STRINGBLOCK) {
            mechanic = new StringBlockMechanic(this, section);
        } else mechanic = null;

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
