package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class MusicDiscMechanicFactory extends MechanicFactory {

    private static MusicDiscMechanicFactory instance;

    public MusicDiscMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MusicDiscListener(this));
    }

    public static MusicDiscMechanicFactory get() {
        return instance;
    }

    @Override
    public MusicDiscMechanic parse(ConfigurationSection section) {
        MusicDiscMechanic mechanic = new MusicDiscMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public MusicDiscMechanic getMechanic(String itemID) {
        return (MusicDiscMechanic) super.getMechanic(itemID);
    }

    @Override
    public MusicDiscMechanic getMechanic(ItemStack itemStack) {
        return (MusicDiscMechanic) super.getMechanic(itemStack);
    }
}
