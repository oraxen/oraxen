package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip;

import org.bukkit.configuration.ConfigurationSection;

public class LogStripping {

    private final String strippedLog;
    private final String dropFromStrippedLog;
    private final boolean decreaseAxeDurability;

    public LogStripping(ConfigurationSection logStripSection) {
        strippedLog = logStripSection.getString("stripped_log");
        dropFromStrippedLog = logStripSection.getString("drop");
        decreaseAxeDurability = logStripSection.getBoolean("decrease_axe_durability");
    }

    public boolean canBeStripped() { return strippedLog != null; }

    public String getStrippedLogBlock() { return strippedLog; }

    public boolean hasStrippedDrop() { return dropFromStrippedLog != null; }

    public String getStrippedLogDrop() { return dropFromStrippedLog; }

    public boolean shouldDecreaseAxeDurability() {
        return decreaseAxeDurability;
    }
}
