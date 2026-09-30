package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.mechanics.ConfigPropertyValues;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class MiscMechanic extends Mechanic {
    private final boolean disableVanillaInteractions;
    private final boolean canStripLogs;
    private final boolean piglinsIgnoreWhenEquipped;
    private final boolean compostable;
    private final boolean preventRenaming;

    private final boolean allowInVanillaRecipes;

    public MiscMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        disableVanillaInteractions = ConfigPropertyValues.bool(MiscMechanicFactory.class, section, MiscMechanicFactory.PROP_DISABLE_VANILLA_INTERACTIONS);
        canStripLogs = ConfigPropertyValues.bool(MiscMechanicFactory.class, section, MiscMechanicFactory.PROP_CAN_STRIP_LOGS);
        piglinsIgnoreWhenEquipped = ConfigPropertyValues.bool(MiscMechanicFactory.class, section, MiscMechanicFactory.PROP_PIGLINS_IGNORE_WHEN_EQUIPPED);
        compostable = ConfigPropertyValues.bool(MiscMechanicFactory.class, section, MiscMechanicFactory.PROP_COMPOSTABLE);
        preventRenaming = ConfigPropertyValues.bool(MiscMechanicFactory.class, section, MiscMechanicFactory.PROP_PREVENT_RENAMING);
        allowInVanillaRecipes = ConfigPropertyValues.bool(MiscMechanicFactory.class, section, MiscMechanicFactory.PROP_ALLOW_IN_VANILLA_RECIPES);
    }

    public boolean isVanillaInteractionDisabled() { return disableVanillaInteractions; }
    public boolean canStripLogs() { return canStripLogs; }
    public boolean piglinIgnoreWhenEquipped() { return piglinsIgnoreWhenEquipped; }
    public boolean isCompostable() { return compostable; }
    public boolean preventsRenaming() { return preventRenaming; }

    public boolean isAllowedInVanillaRecipes() { return allowInVanillaRecipes; }
}
