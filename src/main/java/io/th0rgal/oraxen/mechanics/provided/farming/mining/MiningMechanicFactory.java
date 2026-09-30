package io.th0rgal.oraxen.mechanics.provided.farming.mining;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MiningMechanicFactory extends MechanicFactory {

    private final boolean callEvents;

    public MiningMechanicFactory(ConfigurationSection section) {
        super(section);
        if (PluginUtils.isEnabled("AdvancedEnchantments") && section.getBoolean("call_events", true)) {
            Logs.logError("AdvancedEnchantments is enabled, disabling mining BlockBreakEvent calls");
            section.set("call_events", false);
            OraxenYaml.saveConfig(OraxenPlugin.get().getDataFolder().toPath().resolve("mechanics.yml").toFile(), section);
            callEvents = false;
        } else callEvents = section.getBoolean("call_events", true);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new MiningMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        MiningMechanic mechanic = new MiningMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    @Override
    public Mechanic parse(String itemID, List<?> entries) {
        MiningMechanic mechanic = new MiningMechanic(this, itemID, entries);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean callEvents() {
        return callEvents;
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "farming";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Mines blocks at world offsets, or in a face-relative area when radius and depth are set";
    }
}
