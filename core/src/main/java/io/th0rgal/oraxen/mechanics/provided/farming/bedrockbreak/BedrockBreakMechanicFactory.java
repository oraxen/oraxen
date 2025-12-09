package io.th0rgal.oraxen.mechanics.provided.farming.bedrockbreak;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BedrockBreakMechanicFactory extends MechanicFactory {

    private final boolean disabledOnFirstLayer;
    private final int durabilityCost;

    public BedrockBreakMechanicFactory(ConfigurationSection section) {
        super(section);
        disabledOnFirstLayer = section.getBoolean("disable_on_first_layer");
        durabilityCost = section.getInt("durability_cost");
        new BedrockBreakMechanicManager(this);
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        Mechanic mechanic = new BedrockBreakMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }

    public boolean isDisabledOnFirstLayer() {
        return disabledOnFirstLayer;
    }

    public int getDurabilityCost() {
        return durabilityCost;
    }

    @Override
    public @Nullable String getMechanicCategory() {
        return "farming";
    }

    @Override
    public @Nullable String getMechanicDescription() {
        return "Allows breaking bedrock blocks (requires ProtocolLib)";
    }

    @Override
    public @NotNull List<MechanicConfigProperty> getConfigSchema() {
        return List.of(
                MechanicConfigProperty.integer("delay", "Initial delay in ticks before breaking starts", 0, 0),
                MechanicConfigProperty.integer("hardness", "Break hardness/period in ticks", 1, 1),
                MechanicConfigProperty.decimal("probability", "Chance to break bedrock (0-1)", 1.0, 0.0, 1.0)
        );
    }
}