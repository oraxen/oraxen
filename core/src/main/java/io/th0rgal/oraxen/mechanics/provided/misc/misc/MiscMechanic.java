package io.th0rgal.oraxen.mechanics.provided.misc.misc;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

public class MiscMechanic extends Mechanic {
    private final boolean cactusBreaks;
    private final boolean burnsInFire;
    private final boolean burnsInLava;
    private final boolean disableVanillaInteractions;
    private final boolean canStripLogs;
    private final boolean piglinsIgnoreWhenEquipped;
    private final boolean compostable;

    private final boolean allowInVanillaRecipes;

    public MiscMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section);
        cactusBreaks = section.getBoolean("breaks_from_cactus", true);
        burnsInFire = section.getBoolean("burns_in_fire", true);
        burnsInLava = section.getBoolean("burns_in_lava", true);
        disableVanillaInteractions = section.getBoolean("disable_vanilla_interactions", false);
        canStripLogs = section.getBoolean("can_strip_logs", false);
        piglinsIgnoreWhenEquipped = section.getBoolean("piglins_ignore_when_equipped", false);
        compostable = section.getBoolean("compostable", false);
        allowInVanillaRecipes = section.getBoolean("allow_in_vanilla_recipes", false);

        if (VersionUtil.atOrAbove("1.20.5") && (burnsInFire || burnsInLava)) {
            Logs.logWarning(getItemID() + " seems to be using " + (burnsInFire ? "burns_in_fire" : "burns_in_lava") + " which is deprecated....");
            Logs.logWarning("It is heavily advised to swap to the new fire_resistant-property on all 1.20.5+ servers");
        }
    }

    public boolean breaksFromCactus() { return cactusBreaks; }
    public boolean burnsInFire() { return burnsInFire; }
    public boolean burnsInLava() { return burnsInLava; }
    public boolean isVanillaInteractionDisabled() { return disableVanillaInteractions; }
    public boolean canStripLogs() { return canStripLogs; }
    public boolean piglinIgnoreWhenEquipped() { return piglinsIgnoreWhenEquipped; }
    public boolean isCompostable() { return compostable; }

    public boolean isAllowedInVanillaRecipes() { return allowInVanillaRecipes; }
}
