package io.th0rgal.oraxen.mechanics.provided.misc.music_disc;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

@Deprecated(forRemoval = true, since = "1.21")
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
        if (VersionUtil.atOrAbove("1.21")) {
            Logs.logWarning(mechanic.getItemID() + " is using deprecated Music-Disc-Mechanic...");
            Logs.logWarning("It is heavily advised to swap to the new `jukeboxPlayable`-property on 1.21+ servers...");
            Logs.logWarning("Requires a datapack aswell which Oraxen does not handle at the moment...");
        }
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
