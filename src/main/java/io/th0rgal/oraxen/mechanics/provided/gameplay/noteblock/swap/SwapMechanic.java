package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.swap;

import org.bukkit.configuration.ConfigurationSection;

public class SwapMechanic {
    public final String action;
    public final String switchedBlock;

    public SwapMechanic(ConfigurationSection swapSection) {
        action = swapSection.getString("action");
        switchedBlock = swapSection.getString("switchedBlock");
    }

    public String getAction() { return action; }
    public String getSwitchedBlock() { return switchedBlock; }
}