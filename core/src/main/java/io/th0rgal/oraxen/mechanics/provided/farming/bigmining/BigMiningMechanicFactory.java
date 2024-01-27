package io.th0rgal.oraxen.mechanics.provided.farming.bigmining;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

public class BigMiningMechanicFactory extends MechanicFactory {

    private final boolean callEvents;

    public BigMiningMechanicFactory(ConfigurationSection section) {
        super(section);
        if (PluginUtils.isEnabled("AdvancedEnchantments") && section.getBoolean("call_events", true)) {
            Logs.logError("AdvancedEnchantment is enabled, disabling BigMining-Mechanic");
            section.set("call_events", false);
            OraxenYaml.saveConfig(OraxenPlugin.get().getDataFolder().toPath().resolve("mechanics.yml").toFile(), section);
            this.callEvents = false;
        } else this.callEvents = section.getBoolean("call_events", true);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new BigMiningMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BigMiningMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean callEvents() {
        return callEvents;
    }

}
