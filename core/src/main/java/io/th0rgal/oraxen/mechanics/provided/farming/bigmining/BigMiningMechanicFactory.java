package io.th0rgal.oraxen.mechanics.provided.farming.bigmining;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

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
    public BigMiningMechanic parse(ConfigurationSection section) {
        BigMiningMechanic mechanic = new BigMiningMechanic(this, section);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public BigMiningMechanic getMechanic(String itemID) {
        return (BigMiningMechanic) super.getMechanic(itemID);
    }

    @Override
    public BigMiningMechanic getMechanic(ItemStack itemStack) {
        return (BigMiningMechanic) super.getMechanic(itemStack);
    }

    public boolean callEvents() {
        return callEvents;
    }

}
