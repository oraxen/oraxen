package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.configuration.ConfigurationSection;

public class MiscMechanic extends Mechanic {
    private final boolean cactusBreaks;
    private final boolean burnsInFire;
    private final boolean burnsInLava;
    private final boolean disableVanillaInteractions;

    public MiscMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        cactusBreaks = section.getBoolean("breaks_from_cactus", true);
        burnsInFire = section.getBoolean("burns_in_fire", true);
        burnsInLava = section.getBoolean("burns_in_lava", true);
        disableVanillaInteractions = section.getBoolean("disable_vanilla_interactions", false);
    }

    public boolean breaksFromCactus() { return cactusBreaks; }
    public boolean burnsInFire() { return burnsInFire; }
    public boolean burnsInLava() { return burnsInLava; }
    public boolean isVanillaInteractionDisabled() { return disableVanillaInteractions; }
}
