package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

@Deprecated(forRemoval = true, since = "1.21")
public class MusicDiscMechanicFactory extends MechanicFactory {

    public MusicDiscMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MusicDiscListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection section) {
        Mechanic mechanic = new MusicDiscMechanic(this, section);
        if (VersionUtil.atOrAbove("1.21")) {
            Logs.logWarning(mechanic.getItemID() + " is using deprecated Music-Disc-Mechanic...");
            Logs.logWarning("It is heavily advised to swap to the new `jukeboxPlayable`-property on 1.21+ servers...");
            Logs.logWarning("Requires a datapack aswell which Oraxen does not handle at the moment...");
        }
        addToImplemented(mechanic);
        return mechanic;
    }
}
