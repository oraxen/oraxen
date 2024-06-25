package io.th0rgal.oraxen.mechanics.provided.gameplay.efficiency;

import com.comphenix.protocol.ProtocolLibrary;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class EfficiencyMechanicFactory extends MechanicFactory {

    private static EfficiencyMechanicFactory instance;

    public EfficiencyMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        if (PluginUtils.isEnabled("ProtocolLib")) {
            ProtocolLibrary.getProtocolManager().getPacketListeners().stream().filter(l -> l.getClass().equals(EfficiencyMechanicListener.class))
                            .findFirst().ifPresent(l -> ProtocolLibrary.getProtocolManager().removePacketListener(l));
            ProtocolLibrary.getProtocolManager().addPacketListener(new EfficiencyMechanicListener());
        }
    }

    @Override
    public EfficiencyMechanic parse(ConfigurationSection section) {
        EfficiencyMechanic mechanic = new EfficiencyMechanic(this, section);
        if (VersionUtil.atOrAbove("1.20.5")) {
            Logs.logWarning(mechanic.getItemID() + " is using deprecated Efficiency-Mechanic...");
            Logs.logWarning("It is heavily advised to swap to the new `tool`-property on 1.20.5+ servers...");
        }
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public EfficiencyMechanic getMechanic(String itemID) {
        return (EfficiencyMechanic) super.getMechanic(itemID);
    }

    @Override
    public EfficiencyMechanic getMechanic(ItemStack itemStack) {
        return (EfficiencyMechanic) super.getMechanic(itemStack);
    }

    public static EfficiencyMechanicFactory get() {
        return instance;
    }

}
