package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.configuration.ConfigurationSection;

public class MusicDiscMechanicFactory extends MechanicFactory {

    public MusicDiscMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MusicDiscListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new MusicDiscMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }
}
